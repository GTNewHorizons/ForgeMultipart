package codechicken.microblock.handler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.INetHandlerPlayServer;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustom.IHandshakeHandler;
import codechicken.lib.packet.PacketCustom.IServerPacketHandler;
import codechicken.microblock.MicroMaterialRegistry;

public final class MicroblockSPH$ extends MicroblockPH implements IServerPacketHandler, IHandshakeHandler {

    public static final MicroblockSPH$ MODULE$ = new MicroblockSPH$();

    private MicroblockSPH$() {}

    @Override
    public void handlePacket(PacketCustom packet, EntityPlayerMP sender, INetHandlerPlayServer netHandler) {}

    @Override
    public void handshakeRecieved(NetHandlerPlayServer netHandler) {
        PacketCustom packet = new PacketCustom(registryChannel(), 1);
        MicroMaterialRegistry.writeIDMap(packet);
        netHandler.sendPacket(packet.toPacket());
    }
}
