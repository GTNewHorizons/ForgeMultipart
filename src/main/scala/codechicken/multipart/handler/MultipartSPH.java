package codechicken.multipart.handler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Iterator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import codechicken.lib.data.MCDataOutputWrapper;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.BlockCoord;

public final class MultipartSPH {

    private MultipartSPH() {}

    public static class MCByteStream extends MCDataOutputWrapper {

        private final ByteArrayOutputStream bout;

        public MCByteStream(ByteArrayOutputStream bout) {
            super(new DataOutputStream(bout));
            this.bout = bout;
        }

        public byte[] getBytes() {
            return bout.toByteArray();
        }
    }

    public static MultipartMod$ channel() {
        return MultipartSPH$.MODULE$.channel();
    }

    public static String registryChannel() {
        return MultipartSPH$.MODULE$.registryChannel();
    }

    public static void handlePacket(PacketCustom packet, EntityPlayerMP sender, INetHandlerPlayServer netHandler) {
        MultipartSPH$.MODULE$.handlePacket(packet, sender, netHandler);
    }

    public static void handshakeRecieved(NetHandlerPlayServer netHandler) {
        MultipartSPH$.MODULE$.handshakeRecieved(netHandler);
    }

    public static void onWorldUnload(World world) {
        MultipartSPH$.MODULE$.onWorldUnload(world);
    }

    public static MCByteStream getTileStream(World world, BlockCoord position) {
        return MultipartSPH$.MODULE$.getTileStream(world, position);
    }

    public static void onTickEnd(scala.collection.Seq<EntityPlayerMP> players) {
        MultipartSPH$.MODULE$.onTickEnd(players);
    }

    public static void onChunkWatch(EntityPlayer player, ChunkCoordIntPair chunk) {
        MultipartSPH$.MODULE$.onChunkWatch(player, chunk);
    }

    public static void onChunkUnWatch(EntityPlayer player, ChunkCoordIntPair chunk) {
        MultipartSPH$.MODULE$.onChunkUnWatch(player, chunk);
    }

    public static PacketCustom getDescPacket(Chunk chunk, Iterator<TileEntity> tiles) {
        return MultipartSPH$.MODULE$.getDescPacket(chunk, tiles);
    }
}
