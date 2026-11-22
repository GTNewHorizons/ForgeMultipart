package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.Cuboid6;
import codechicken.microblock.FaceMicroClass$;
import codechicken.microblock.HollowMicroblockClient;
import codechicken.microblock.MicroblockGenerator;
import codechicken.microblock.PostMicroblockClient;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultipartHelper;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.TileMultipartClient;
import codechicken.multipart.examples.PartRegistrationExample;
import codechicken.multipart.examples.PartRegistrationExample.ExamplePart;
import codechicken.multipart.handler.MultipartSPH;
import codechicken.multipart.handler.MultipartSaveLoad;
import codechicken.multipart.minecraft.ButtonPart;
import codechicken.multipart.minecraft.TorchPart;
import io.netty.buffer.ByteBuf;
import scala.collection.JavaConversions;

/**
 * The half of MultipartHelper a plain JVM test cannot reach. Tile construction runs the ASM generator, the NBT path
 * needs the save/load hooks, and the packet path needs a loaded chunk.
 */
class MultipartHelperFunctionalTest {

    private static final String WORLD_DEPENDENT_PART = "test:preview_world_dependent";
    private static final String PREVIEW_SERVER_FACE = "test:preview_server_face";
    private static final String PREVIEW_HOLLOW_PROBE = "test:preview_hollow_probe";
    private static final String PREVIEW_POST_PROBE = "test:preview_post_probe";

    static void registerDuringInit() {
        MultiPartRegistry.registerPartFactory(new MultiPartRegistry.IPartFactory2() {

            @Override
            public TMultiPart createPart(String name, NBTTagCompound nbt) {
                return new WorldDependentPart();
            }

            @Override
            public TMultiPart createPart(String name, MCDataInput packet) {
                return new WorldDependentPart();
            }

            @Override
            public TMultiPart createPartForClientPreview(String name, NBTTagCompound nbt) {
                return new WorldDependentPart();
            }
        }, WORLD_DEPENDENT_PART);
        registerPreviewFactory(
                PREVIEW_SERVER_FACE,
                () -> MicroblockGenerator.create(FaceMicroClass$.MODULE$, 0, false));
        registerPreviewFactory(PREVIEW_HOLLOW_PROBE, () -> new HollowMicroblockClientFunctionalTest.ClientProbe(0));
        registerPreviewFactory(PREVIEW_POST_PROBE, () -> new PostMicroblockClientFunctionalTest.ClientProbe(0));
    }

    @Test
    void buildsATileAroundTheGivenParts() {
        TMultiPart torch = MultiPartRegistry.loadPart("mc_torch", null);
        assertNotNull(torch);

        TileMultipart tile = MultipartHelper.createTileFromParts(Collections.singletonList(torch));

        assertNotNull(tile);
        assertEquals(1, tile.jPartList().size());
        assertSame(torch, tile.jPartList().get(0));
        assertSame(tile, torch.tile());
    }

    /**
     * Multipart tiles all save themselves under the id "savedMultipart", which is the whole reason this entry point
     * exists: the tile has to be rebuilt from NBT without waiting for the ChunkLoad event.
     */
    @Test
    void rebuildsAnOrderedSlottedTileFromItsOwnNbtAndPublishesTheLoadingWorld() {
        World world = world();
        TileMultipart saved = mixedTile();

        NBTTagCompound tag = new NBTTagCompound();
        saved.writeToNBT(tag);
        assertEquals("savedMultipart", tag.getString("id"));
        NBTTagList savedParts = tag.getTagList("parts", 10);
        assertEquals(2, savedParts.tagCount());
        assertEquals("mc_torch", savedParts.getCompoundTagAt(0).getString("id"));
        assertEquals(5, savedParts.getCompoundTagAt(0).getByte("meta"));
        assertEquals("mc_button", savedParts.getCompoundTagAt(1).getString("id"));
        assertEquals(1, savedParts.getCompoundTagAt(1).getByte("meta"));

        MultipartSaveLoad.loadingWorld_$eq(null);
        TileEntity loaded = MultipartHelper.createTileFromNBT(world, tag);

        TileMultipart tile = assertInstanceOf(TileMultipart.class, loaded);
        assertEquals(2, tile.jPartList().size());
        assertEquals("mc_torch", tile.jPartList().get(0).getType());
        assertEquals("mc_button", tile.jPartList().get(1).getType());
        assertSame(tile.jPartList().get(0), tile.partMap(0));
        assertSame(tile.jPartList().get(1), tile.partMap(4));
        assertSame(world, MultipartSaveLoad.loadingWorld());
    }

    @Test
    void buildsAWorldlessClientPreviewFromSavedNbt() {
        NBTTagCompound tag = previewTag(37);

        TileMultipart tile = MultipartHelper.createPreviewTileFromNBT(null, tag);

        assertTrue(tile instanceof TileMultipartClient);
        assertNull(tile.getWorldObj());
        assertEquals(31, tile.xCoord);
        assertEquals(72, tile.yCoord);
        assertEquals(-9, tile.zCoord);
        assertEquals(1, tile.jPartList().size());
        ExamplePart part = assertInstanceOf(ExamplePart.class, tile.jPartList().get(0));
        assertSame(tile, part.tile());
        assertEquals(37, part.value());
    }

    /** Parts may assume a world in onPartChanged, as ProjectRed gates do; worldless previews must not call it. */
    @Test
    void worldlessPreviewDoesNotCallPartChangeCallbacks() {
        NBTTagCompound part = new NBTTagCompound();
        part.setString("id", WORLD_DEPENDENT_PART);
        NBTTagCompound tag = previewTag(37);
        tag.getTagList("parts", 10).appendTag(part);

        TileMultipart tile = MultipartHelper.createPreviewTileFromNBT(null, tag);

        assertNotNull(tile);
        assertEquals(2, tile.jPartList().size());
        WorldDependentPart worldDependent = assertInstanceOf(WorldDependentPart.class, tile.jPartList().get(1));
        assertSame(tile, worldDependent.tile());
    }

    /**
     * Stock microblocks only compute render bounds in part callbacks, which worldless previews skip. The dedicated test
     * server strips the generated client microblock traits, so the client parts here are the hand-written probes.
     */
    @Test
    void worldlessPreviewInitializesMicroblockRenderBounds() {
        NBTTagList parts = new NBTTagList();
        parts.appendTag(microblockTag(PREVIEW_SERVER_FACE, 1 << 4)); // bottom cover
        parts.appendTag(microblockTag(PREVIEW_HOLLOW_PROBE, 1 << 4 | 2)); // north hollow cover
        parts.appendTag(microblockTag(PREVIEW_POST_PROBE, 2 << 4)); // Y-axis post
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "savedMultipart");
        tag.setTag("parts", parts);

        TileMultipart tile = MultipartHelper.createPreviewTileFromNBT(null, tag);

        assertNotNull(tile);
        HollowMicroblockClient hollow = assertInstanceOf(HollowMicroblockClient.class, tile.jPartList().get(1));
        PostMicroblockClient post = assertInstanceOf(PostMicroblockClient.class, tile.jPartList().get(2));
        assertNotNull(hollow.renderBounds());
        assertEquals(hollow.getHollowSize(), hollow.renderMask() >> 8);
        assertNotNull(post.renderBounds1());
        // Clipped by the bottom cover, so the bounds were computed against the bound tile.
        assertEquals(0.125, post.renderBounds1().min.y, 1e-12);

        Cuboid6 postBounds = post.renderBounds1().copy();
        // The world-backed path computes the same post bounds through onPartChanged.
        tile.internalPartChange(null);
        assertBounds(postBounds, post.renderBounds1());
    }

    @Test
    void rejectsServerWorldsBeforeConstructingPreviewParts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MultipartHelper.createPreviewTileFromNBT(world(), previewTag(37)));
    }

    @Test
    void writesTheExactMixedPartChunkDescriptionPacket() {
        World world = world();
        Chunk chunk = world.getChunkFromChunkCoords(1, 2);
        TileMultipart tile = mixedTile();
        tile.xCoord = 18;
        tile.yCoord = 200;
        tile.zCoord = 35;

        PacketCustom packet = MultipartSPH.getDescPacket(chunk, Collections.<TileEntity>singletonList(tile).iterator());
        ByteBuf buffer = packet.getByteBuf();
        byte[] actual = new byte[buffer.writerIndex()];
        buffer.getBytes(0, actual);

        // type | chunk x/z | tile count | local x/y/z | part count | torch id/meta | button id/meta
        assertArrayEquals(
                new byte[] { 2, 0, 0, 0, 1, 0, 0, 0, 2, 0, 1, 2, 0, 0, 0, (byte) 200, 3, 2, 3, 5, 0, 1 },
                actual);
    }

    /** The id guard runs before anything else, so a foreign tag must not disturb the loading world. */
    @Test
    void leavesTheLoadingWorldAloneForATagItDoesNotOwn() {
        World world = world();
        MultipartSaveLoad.loadingWorld_$eq(world);

        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "somethingElse");

        assertNull(MultipartHelper.createTileFromNBT(null, tag));
        assertSame(world, MultipartSaveLoad.loadingWorld());
    }

    @Test
    void registersATileConverterOntoTheSaveLoadList() {
        MarkerConverter converter = new MarkerConverter();

        MultipartHelper.registerTileConverter(converter);

        List<?> converters = JavaConversions.seqAsJavaList(MultipartSaveLoad.converters());
        assertTrue(converters.contains(converter), "registerTileConverter must append to MultipartSaveLoad.converters");
    }

    /** Needs a loaded chunk. With no players watching, the send itself is a no-op, but the packet is still built. */
    @Test
    void sendsADescPacketForATileInALoadedChunk() {
        World world = world();
        TMultiPart torch = MultiPartRegistry.loadPart("mc_torch", null);
        TileMultipart tile = MultipartHelper.createTileFromParts(Collections.singletonList(torch));
        tile.setWorldObj(world);
        tile.xCoord = 0;
        tile.yCoord = 64;
        tile.zCoord = 0;

        assertNotNull(world.getChunkFromBlockCoords(0, 0));
        assertDoesNotThrow(() -> MultipartHelper.sendDescPacket(world, tile));
    }

    private static World world() {
        World world = MinecraftServer.getServer().worldServers[0];
        assertNotNull(world);
        return world;
    }

    private static TileMultipart mixedTile() {
        return MultipartHelper.createTileFromParts(Arrays.asList(new TorchPart(5), new ButtonPart(1)));
    }

    private static NBTTagCompound previewTag(int value) {
        NBTTagCompound part = new NBTTagCompound();
        part.setString("id", PartRegistrationExample.PART_TYPE);
        part.setInteger("value", value);
        NBTTagList parts = new NBTTagList();
        parts.appendTag(part);

        NBTTagCompound tile = new NBTTagCompound();
        tile.setString("id", "savedMultipart");
        tile.setInteger("x", 31);
        tile.setInteger("y", 72);
        tile.setInteger("z", -9);
        tile.setTag("parts", parts);
        return tile;
    }

    private static void registerPreviewFactory(String name, Supplier<TMultiPart> previewPart) {
        MultiPartRegistry.registerPartFactory(new MultiPartRegistry.IPartFactory2() {

            @Override
            public TMultiPart createPart(String name, NBTTagCompound nbt) {
                return null;
            }

            @Override
            public TMultiPart createPart(String name, MCDataInput packet) {
                return null;
            }

            @Override
            public TMultiPart createPartForClientPreview(String name, NBTTagCompound nbt) {
                return previewPart.get();
            }
        }, name);
    }

    private static NBTTagCompound microblockTag(String id, int shape) {
        NBTTagCompound part = new NBTTagCompound();
        part.setString("id", id);
        part.setByte("shape", (byte) shape);
        part.setString("material", "minecraft:stone");
        return part;
    }

    private static void assertBounds(Cuboid6 expected, Cuboid6 actual) {
        assertEquals(expected.min.x, actual.min.x, 1e-12);
        assertEquals(expected.min.y, actual.min.y, 1e-12);
        assertEquals(expected.min.z, actual.min.z, 1e-12);
        assertEquals(expected.max.x, actual.max.x, 1e-12);
        assertEquals(expected.max.y, actual.max.y, 1e-12);
        assertEquals(expected.max.z, actual.max.z, 1e-12);
    }

    private static final class WorldDependentPart extends TMultiPart {

        @Override
        public String getType() {
            return WORLD_DEPENDENT_PART;
        }

        @Override
        public void onPartChanged(TMultiPart part) {
            world().getTotalWorldTime();
        }
    }

    /** Matches only its own tile class, so registering it cannot affect any real chunk load. */
    private static final class MarkerTile extends TileEntity {
    }

    private static final class MarkerConverter extends MultipartHelper.IPartTileConverter<MarkerTile> {

        MarkerConverter() {
            super(MarkerTile.class);
        }

        @Override
        public TMultiPart convertOne(MarkerTile tile) {
            return null;
        }
    }
}
