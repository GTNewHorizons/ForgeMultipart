package codechicken.microblock.handler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.INetHandlerPlayServer;

import codechicken.lib.packet.PacketCustom;

public final class MicroblockSPH {

    private MicroblockSPH() {}

    public static String registryChannel() {
        return MicroblockSPH$.MODULE$.registryChannel();
    }

    public static void handlePacket(PacketCustom packet, EntityPlayerMP sender, INetHandlerPlayServer netHandler) {
        MicroblockSPH$.MODULE$.handlePacket(packet, sender, netHandler);
    }

    public static void handshakeRecieved(NetHandlerPlayServer netHandler) {
        MicroblockSPH$.MODULE$.handshakeRecieved(netHandler);
    }
}
