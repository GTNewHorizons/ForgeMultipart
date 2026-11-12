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

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.junit.jupiter.api.Test;

import codechicken.lib.packet.PacketCustom;
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
