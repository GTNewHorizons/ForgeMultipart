package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultipartHelper;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.handler.MultipartSaveLoad;
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
    void rebuildsATileFromItsOwnNbtAndPublishesTheLoadingWorld() {
        World world = world();
        TMultiPart torch = MultiPartRegistry.loadPart("mc_torch", null);
        TileMultipart saved = MultipartHelper.createTileFromParts(Collections.singletonList(torch));

        NBTTagCompound tag = new NBTTagCompound();
        saved.writeToNBT(tag);
        assertEquals("savedMultipart", tag.getString("id"));

        MultipartSaveLoad.loadingWorld_$eq(null);
        TileEntity loaded = MultipartHelper.createTileFromNBT(world, tag);

        TileMultipart tile = assertInstanceOf(TileMultipart.class, loaded);
        assertEquals(1, tile.jPartList().size());
        assertEquals("mc_torch", tile.jPartList().get(0).getType());
        assertSame(world, MultipartSaveLoad.loadingWorld());
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
