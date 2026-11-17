package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import net.minecraft.util.IIcon;

import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.HollowMicroClass$;
import codechicken.microblock.HollowMicroblock$class;
import codechicken.microblock.HollowMicroblockClient;
import codechicken.microblock.HollowMicroblockClient$class;
import codechicken.microblock.ISidedHollowConnect;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.microblock.Microblock;
import codechicken.microblock.TMicroOcclusionClient$class;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.Function5;
import scala.runtime.AbstractFunction5;
import scala.runtime.BoxedUnit;

class HollowMicroblockClientFunctionalTest {

    @Test
    void realBoundsAndLiveCenterConnectorDriveClientMaskAndRimsOnAllFortyTwoShapes() {
        ClientProbe part = new ClientProbe(MicroMaterialRegistry.materialID("minecraft:stone"));
        SlotTile tile = new SlotTile();
        part.bind(tile);
        for (int size = 1; size <= 7; size++) {
            for (int side = 0; side < 6; side++) {
                part.setShape(size, side);
                for (int opening : new int[] { 1, 8, 11 }) {
                    tile.connector.size = opening;
                    part.recalcBounds();
                    assertEquals(opening << 8, part.renderMask());
                    assertEquals(side, tile.connector.side);
                    Cuboid6 physical = part.getBounds();
                    assertNotSame(physical, part.renderBounds());
                    assertBounds(physical, part.renderBounds());
                    int[] calls = { 0 };
                    double[] volume = { 0 };
                    part.renderHollow(
                            new Vector3(),
                            0,
                            part.renderBounds(),
                            0,
                            false,
                            new AbstractFunction5<Vector3, IMicroMaterial, Object, Cuboid6, Object, BoxedUnit>() {

                                @Override
                                public BoxedUnit apply(Vector3 pos, IMicroMaterial material, Object pass, Cuboid6 box,
                                        Object mask) {
                                    assertSame(part.getIMaterial(), material);
                                    assertEquals(0, pass);
                                    // The first two callbacks draw internal walls; the last four span the rim volume.
                                    if (calls[0]++ >= 2) volume[0] += (box.max.x - box.min.x) * (box.max.y - box.min.y)
                                            * (box.max.z - box.min.z);
                                    return BoxedUnit.UNIT;
                                }
                            });
                    assertEquals(6, calls[0]);
                    assertEquals(size / 8D * (1 - opening / 16D * (opening / 16D)), volume[0], 1e-12);
                }
                tile.connected = false;
                part.recalcBounds();
                assertEquals(8 << 8, part.renderMask());
                tile.connected = true;
            }
        }
    }

    @Test
    void occlusionClientRefreshesClippingAndMasksWithLiveStoneAndGlassNeighbors() {
        int stone = MicroMaterialRegistry.materialID("minecraft:stone");
        int glass = MicroMaterialRegistry.materialID("minecraft:glass");
        assertNotEquals(stone, glass);
        ClientProbe part = new ClientProbe(stone);
        ClientProbe neighbor = new ClientProbe(stone);
        SlotTile tile = new SlotTile();
        part.bind(tile);
        tile.connector.size = 8;
        part.setShape(2, 4);
        neighbor.setShape(1, 0);
        tile.neighbor = neighbor;
        TMicroOcclusionClient$class.recalcBounds(part);
        assertEquals(1, part.renderMask());
        assertEquals(0, part.renderBounds().min.y);
        Cuboid6 original = part.getBounds().copy();
        neighbor.setShape(4, 0);
        part.recalcBounds();
        assertEquals(8 << 8, part.renderMask());
        assertEquals(0.5, part.renderBounds().min.y);
        assertBounds(original, part.getBounds());
        neighbor.material_$eq(glass);
        TMicroOcclusionClient$class.recalcBounds(part);
        assertEquals(0, part.renderMask());
        assertBounds(original, part.renderBounds());
        part.material_$eq(glass);
        neighbor.material_$eq(stone);
        neighbor.setShape(1, 0);
        TMicroOcclusionClient$class.recalcBounds(part);
        assertEquals(0.125, part.renderBounds().min.y);
        assertEquals(0, part.renderMask());
        tile.neighbor = null;
        TMicroOcclusionClient$class.recalcBounds(part);
        assertNotSame(part.getBounds(), part.renderBounds());
        assertBounds(original, part.renderBounds());
        assertEquals(0, part.renderMask());
    }

    private static void assertBounds(Cuboid6 expected, Cuboid6 actual) {
        assertArrayEquals(
                new double[] { expected.min.x, expected.min.y, expected.min.z, expected.max.x, expected.max.y,
                        expected.max.z },
                new double[] { actual.min.x, actual.min.y, actual.min.z, actual.max.x, actual.max.y, actual.max.z },
                1e-12);
    }

    private static final class Connector extends TMultiPart implements ISidedHollowConnect {

        int size;
        int side;

        @Override
        public String getType() {
            return "test:hollow_connector";
        }

        @Override
        public int getHollowSize(int side) {
            this.side = side;
            return size;
        }
    }

    private static final class SlotTile extends TileMultipart {

        final Connector connector = new Connector();
        boolean connected = true;
        TMultiPart neighbor;

        @Override
        public TMultiPart partMap(int slot) {
            return slot == 0 ? neighbor : slot == 6 && connected ? connector : null;
        }
    }

    /** Uses client helpers with real Forge factories; the dedicated server strips client factory creation. */
    private static final class ClientProbe extends Microblock implements HollowMicroblockClient {

        Cuboid6 bounds;
        int mask;

        ClientProbe(int material) {
            super(material);
            HollowMicroblockClient$class.$init$(this);
        }

        @Override
        public HollowMicroClass$ microClass() {
            return HollowMicroClass$.MODULE$;
        }

        @Override
        public int itemClassID() {
            return microClass().getClassId();
        }

        @Override
        public int redstoneConductionMap() {
            return HollowMicroblock$class.redstoneConductionMap(this);
        }

        @Override
        public int getSlotMask() {
            return 1 << getSlot();
        }

        @Override
        public boolean edgeCornerOcclusionTest(codechicken.microblock.TMicroOcclusion edge,
                codechicken.microblock.TMicroOcclusion corner) {
            return codechicken.microblock.TMicroOcclusion$class.edgeCornerOcclusionTest(this, edge, corner);
        }

        @Override
        public List<Cuboid6> getCollisionBoxes() {
            return HollowMicroblock$class.getCollisionBoxes(this);
        }

        @Override
        public List<IndexedCuboid6> getSubParts() {
            return HollowMicroblock$class.getSubParts(this);
        }

        @Override
        public Cuboid6 getBounds() {
            return HollowMicroblock$class.getBounds(this);
        }

        @Override
        public int getSlot() {
            return getShape();
        }

        @Override
        public int getHollowSize() {
            return HollowMicroblock$class.getHollowSize(this);
        }

        @Override
        public List<Cuboid6> getPartialOcclusionBoxes() {
            return HollowMicroblock$class.getPartialOcclusionBoxes(this);
        }

        @Override
        public Iterable<Cuboid6> getOcclusionBoxes() {
            return HollowMicroblock$class.getOcclusionBoxes(this);
        }

        @Override
        public boolean allowCompleteOcclusion() {
            return true;
        }

        @Override
        public boolean solid(int side) {
            return false;
        }

        @Override
        public Cuboid6 renderBounds() {
            return bounds;
        }

        @Override
        public void renderBounds_$eq(Cuboid6 value) {
            bounds = value;
        }

        @Override
        public int renderMask() {
            return mask;
        }

        @Override
        public void renderMask_$eq(int value) {
            mask = value;
        }

        @Override
        public int getPriorityClass() {
            return 0;
        }

        @Override
        public IIcon getBrokenIcon(int side) {
            throw new AssertionError("icon lookup");
        }

        @Override
        public void recalcBounds() {
            HollowMicroblockClient$class.recalcBounds(this);
        }

        @Override
        public void render(Vector3 pos, int pass) {
            HollowMicroblockClient$class.render(this, pos, pass);
        }

        @Override
        public void renderHollow(Vector3 pos, int pass, Cuboid6 c, int sideMask, boolean face,
                Function5<Vector3, IMicroMaterial, Object, Cuboid6, Object, BoxedUnit> f) {
            HollowMicroblockClient$class.renderHollow(this, pos, pass, c, sideMask, face, f);
        }

        public void codechicken$microblock$HollowMicroblockClient$$super$recalcBounds() {
            TMicroOcclusionClient$class.recalcBounds(this);
        }

        public boolean codechicken$microblock$HollowMicroblock$$super$occlusionTest(TMultiPart other) {
            return super.occlusionTest(other);
        }

        public boolean codechicken$microblock$TMicroOcclusion$$super$occlusionTest(TMultiPart other) {
            return super.occlusionTest(other);
        }

        public void codechicken$microblock$TMicroOcclusionClient$$super$onAdded() {
            super.onAdded();
        }

        public void codechicken$microblock$TMicroOcclusionClient$$super$onPartChanged(TMultiPart other) {
            super.onPartChanged(other);
        }

        public void codechicken$microblock$TMicroOcclusionClient$$super$read(MCDataInput packet) {
            super.read(packet);
        }
    }
}
