package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import net.minecraft.util.AxisAlignedBB;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.IRandomDisplayTick;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.TileMultipartClient;
import codechicken.multipart.asm.MultipartMixinFactory;
import codechicken.multipart.scalatraits.TRandomDisplayTickTile;
import scala.collection.JavaConversions;
import scala.collection.immutable.Nil$;

/** Consumer-visible cache and dispatch behavior of the generated client tile traits. */
class TileMultipartClientFunctionalTest {

    @Test
    void renderCachePartitionsOnceInPartOrderAndBuildsTranslatedBounds() throws Exception {
        TileMultipart tile = newClientTile();
        tile.xCoord = 10;
        tile.yCoord = 20;
        tile.zCoord = 30;
        RenderPart dynamicOnly = new RenderPart("dynamic", false, true, new Cuboid6(-0.1, 0.0, 0.0, 0.2, 0.4, 0.2));
        RenderPart fixed = new RenderPart("fixed", false, false, new Cuboid6(0.25, 0.2, 0.3, 0.5, 0.6, 0.7));
        RenderPart ticking = new RenderPart("ticking", true, true, new Cuboid6(0.8, 0.7, 0.6, 1.2, 1.1, 1.3));
        setParts(tile, dynamicOnly, fixed, ticking);

        TileMultipartClient client = (TileMultipartClient) tile;
        client.updateRenderCache();

        assertArrayEquals(new TMultiPart[] { fixed }, staticCache(client));
        assertArrayEquals(new TMultiPart[] { dynamicOnly, ticking }, dynamicCache(client));
        assertTrue(client.hasDynamicParts());
        assertEquals(1, dynamicOnly.doesTickCalls);
        assertEquals(1, dynamicOnly.dynamicCalls);
        assertEquals(1, fixed.doesTickCalls);
        assertEquals(1, fixed.dynamicCalls);
        assertEquals(1, ticking.doesTickCalls);
        assertEquals(0, ticking.dynamicCalls, "doesTick must short-circuit shouldRenderDynamic");
        assertBounds(cachedBounds(client), 9.9, 20.0, 30.0, 11.2, 21.1, 31.3);
    }

    @Test
    void nullPartListProducesEmptyCachesAndTheFullBlockBounds() throws Exception {
        TileMultipart tile = newClientTile();
        tile.xCoord = -3;
        tile.yCoord = 7;
        tile.zCoord = 11;
        tile.partList_$eq(null);

        TileMultipartClient client = (TileMultipartClient) tile;
        client.updateRenderCache();

        assertEquals(0, staticCache(client).length);
        assertEquals(0, dynamicCache(client).length);
        assertFalse(client.hasDynamicParts());
        assertBounds(cachedBounds(client), -3.0, 7.0, 11.0, -2.0, 8.0, 12.0);
    }

    @Test
    void emptyPartsUseFullLocalBoundsAndLazyQueriesPopulateBothCaches() throws Exception {
        TileMultipart tile = newClientTile();
        tile.xCoord = 4;
        tile.yCoord = 5;
        tile.zCoord = 6;
        TileMultipartClient client = (TileMultipartClient) tile;
        assertNull(staticCache(client));
        assertNull(cachedBounds(client));

        assertFalse(client.shouldRenderInPass(17));
        AxisAlignedBB bounds = client.getRenderBoundingBox();

        assertEquals(0, staticCache(client).length);
        assertEquals(0, dynamicCache(client).length);
        assertBounds(bounds, 4.0, 5.0, 6.0, 5.0, 6.0, 7.0);
        assertSame(bounds, cachedBounds(client));
    }

    @Test
    void dynamicRenderingReturnsBeforeTouchingTheCacheWhenTheFlagIsFalse() throws Exception {
        TileMultipartClient client = (TileMultipartClient) newClientTile();
        RenderPart dynamic = new RenderPart("dynamic", true, false, Cuboid6.full);
        setDynamicCache(client, new TMultiPart[] { dynamic });
        client.hasDynamicParts_$eq(false);

        client.renderDynamic(null, 0.5F, 1);

        assertEquals(0, dynamic.renderDynamicCalls);
    }

    @Test
    void baseRandomDisplayTickIsANoOp() {
        TileMultipartClient client = (TileMultipartClient) newClientTile();
        client.randomDisplayTick(new Random(1L));
    }

    @Test
    void randomDisplayTickVisitsOnlyMatchingPartsInOrderWithTheSameRandom() {
        TileMultipart tile = newRandomDisplayTickTile();
        List<String> calls = new ArrayList<>();
        DisplayPart first = new DisplayPart("first", calls);
        RenderPart skipped = new RenderPart("skipped", false, false, Cuboid6.full);
        DisplayPart second = new DisplayPart("second", calls);
        setParts(tile, first, skipped, second);
        Random random = new Random(2L);

        ((TRandomDisplayTickTile) tile).randomDisplayTick(random);

        assertEquals(Arrays.asList("first", "second"), calls);
        assertSame(random, first.lastRandom);
        assertSame(random, second.lastRandom);
    }

    private static TileMultipart newClientTile() {
        int traitId = MultipartMixinFactory.getId(TileMultipartClient.class.getName().replace('.', '/'));
        BitSet traits = new BitSet();
        traits.set(traitId);
        return (TileMultipart) MultipartMixinFactory.construct(traits, Nil$.MODULE$);
    }

    private static TileMultipart newRandomDisplayTickTile() {
        BitSet traits = new BitSet();
        traits.set(MultipartMixinFactory.getId(TileMultipartClient.class.getName().replace('.', '/')));
        traits.set(MultipartMixinFactory.getId(TRandomDisplayTickTile.class.getName().replace('.', '/')));
        return (TileMultipart) MultipartMixinFactory.construct(traits, Nil$.MODULE$);
    }

    private static void setParts(TileMultipart tile, TMultiPart... parts) {
        tile.partList_$eq(JavaConversions.asScalaBuffer(Arrays.asList(parts)).toList());
    }

    private static TMultiPart[] staticCache(TileMultipartClient client) throws Exception {
        return (TMultiPart[]) generatedField(client, "staticCache").get(client);
    }

    private static TMultiPart[] dynamicCache(TileMultipartClient client) throws Exception {
        return (TMultiPart[]) generatedField(client, "dynamicCache").get(client);
    }

    private static void setDynamicCache(TileMultipartClient client, TMultiPart[] parts) throws Exception {
        generatedField(client, "dynamicCache").set(client, parts);
    }

    private static AxisAlignedBB cachedBounds(TileMultipartClient client) throws Exception {
        return (AxisAlignedBB) generatedField(client, "cachedRenderBounds").get(client);
    }

    private static Field generatedField(TileMultipartClient client, String name) throws Exception {
        Field field = client.getClass().getDeclaredField("codechicken$multipart$TileMultipartClient$$" + name);
        field.setAccessible(true);
        return field;
    }

    private static void assertBounds(AxisAlignedBB bounds, double minX, double minY, double minZ, double maxX,
            double maxY, double maxZ) {
        assertEquals(minX, bounds.minX, 0.000001);
        assertEquals(minY, bounds.minY, 0.000001);
        assertEquals(minZ, bounds.minZ, 0.000001);
        assertEquals(maxX, bounds.maxX, 0.000001);
        assertEquals(maxY, bounds.maxY, 0.000001);
        assertEquals(maxZ, bounds.maxZ, 0.000001);
    }

    private static class RenderPart extends TMultiPart {

        private final String name;
        private final boolean ticking;
        private final boolean dynamic;
        private final Cuboid6 bounds;
        private int doesTickCalls;
        private int dynamicCalls;
        private int renderDynamicCalls;

        private RenderPart(String name, boolean ticking, boolean dynamic, Cuboid6 bounds) {
            this.name = name;
            this.ticking = ticking;
            this.dynamic = dynamic;
            this.bounds = bounds;
        }

        @Override
        public String getType() {
            return "client_tile_test:" + name;
        }

        @Override
        public boolean doesTick() {
            doesTickCalls++;
            return ticking;
        }

        @Override
        public boolean shouldRenderDynamic() {
            dynamicCalls++;
            return dynamic;
        }

        @Override
        public Cuboid6 getRenderBounds() {
            return bounds;
        }

        @Override
        public void renderDynamic(codechicken.lib.vec.Vector3 pos, float frame, int pass) {
            renderDynamicCalls++;
        }
    }

    private static final class DisplayPart extends RenderPart implements IRandomDisplayTick {

        private final List<String> calls;
        private Random lastRandom;

        private DisplayPart(String name, List<String> calls) {
            super(name, false, false, Cuboid6.full);
            this.calls = calls;
        }

        @Override
        public void randomDisplayTick(Random random) {
            calls.add(getType().substring("client_tile_test:".length()));
            lastRandom = random;
        }
    }
}
