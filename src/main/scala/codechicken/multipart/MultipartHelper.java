package codechicken.multipart;

import java.util.Arrays;
import java.util.Collections;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import codechicken.lib.packet.PacketCustom;
import codechicken.multipart.handler.MultipartSPH;
import codechicken.multipart.handler.MultipartSaveLoad;
import scala.collection.JavaConversions;

/**
 * Static helper class for handling the unusual way that multipart tile entities load from nbt and send description
 * packets.
 * <p>
 * Multipart tile entities will all save themselves with the id "savedMultipart" which if normally loaded by minecraft,
 * will create a dummy tile entity which just holds the NBT it was read from. These dummies are then converted to actual
 * container tiles on the ChunkLoad event. The createTileFromNBT function should be used to construct a multipart tile
 * from NBT without the ChunkLoad event.
 * <p>
 * Multipart tile entities do not send description packets via the conventional means of one packet per tile when
 * PlayerInstance calls for it, to do so would be terribly inefficient. Instead, the ChunkWatch event is used to batch
 * all the describing data for a chunk into one packet which is compressed using relative positions. The sendDescPacket
 * function should be used to send the description packet of a tile without a ChunkWatch event.
 * <p>
 * An example of using this class to move blocks/tile entites around can be found at
 * www.chickenbones.craftsaddle.org/Files/Other/ItemDevTool2.java
 * <p>
 * The reference carried a commented-out sendDescPackets batching several tiles at once. It was removed for compilation
 * until a PlayerInstance access transformer is pulled into forge, and is not reinstated here.
 */
public final class MultipartHelper {

    private MultipartHelper() {}

    public static TileEntity createTileFromNBT(World world, NBTTagCompound tag) {
        if (!tag.getString("id").equals("savedMultipart")) {
            return null;
        }

        MultipartSaveLoad.loadingWorld_$eq(world);
        return TileMultipart.createFromNBT(tag);
    }

    /**
     * Note. This method should only be used to send tiles that have been created on the server mid-game via an NBT load
     * to clients.
     */
    public static void sendDescPacket(World world, TileEntity tile) {
        Chunk c = world.getChunkFromBlockCoords(tile.xCoord, tile.zCoord);
        PacketCustom pkt = MultipartSPH.getDescPacket(c, Arrays.asList(tile).iterator());
        if (pkt != null) {
            pkt.sendToChunk(world, c.xPosition, c.zPosition);
        }
    }

    public static void registerTileConverter(IPartTileConverter<?> converter) {
        MultipartSaveLoad.converters().$plus$eq(converter);
    }

    public static TileMultipart createTileFromParts(Iterable<TMultiPart> parts) {
        scala.collection.Iterable<TMultiPart> scalaParts = JavaConversions.iterableAsScalaIterable(parts);
        TileMultipart tile = MultipartGenerator$.MODULE$.generateCompositeTile(null, scalaParts, false);
        tile.loadParts(scalaParts);
        return tile;
    }

    /** Helper class for converting tile entities to multiparts on chunk load. */
    public abstract static class IPartTileConverter<T extends TileEntity> {

        private final Class<T> clazz;

        public IPartTileConverter(Class<T> clazz) {
            this.clazz = clazz;
        }

        public Class<T> clazz() {
            return clazz;
        }

        /**
         * @return true if this tile can be converted by this converter. If canConvert returns true, but convertMulti
         *         returns no parts, the tile will be deleted
         */
        public boolean canConvert(TileEntity tile) {
            return clazz.isInstance(tile);
        }

        @SuppressWarnings("unchecked")
        public Iterable<TMultiPart> convert(TileEntity tile) {
            return convertMulti((T) tile);
        }

        /**
         * Convert the TileEntity into multiple parts
         *
         * @return An iterable list of parts or an empty list to delete the tile
         */
        public Iterable<TMultiPart> convertMulti(T tile) {
            TMultiPart part = convertOne(tile);
            return part == null ? Collections.<TMultiPart>emptyList() : Collections.singletonList(part);
        }

        /**
         * Convert the TileEntity into a part
         *
         * @return The converted part, or null to delete the tile
         */
        public abstract TMultiPart convertOne(T tile);
    }
}
