package codechicken.microblock.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.INetHandlerPlayClient;

import codechicken.lib.packet.PacketCustom;

public final class MicroblockCPH {

    private MicroblockCPH() {}

    public static String registryChannel() {
        return MicroblockCPH$.MODULE$.registryChannel();
    }

    public static void handlePacket(PacketCustom packet, Minecraft minecraft, INetHandlerPlayClient netHandler) {
        MicroblockCPH$.MODULE$.handlePacket(packet, minecraft, netHandler);
    }

    public static void handleMaterialRegistration(PacketCustom packet, INetHandlerPlayClient netHandler) {
        MicroblockCPH$.MODULE$.handleMaterialRegistration(packet, netHandler);
    }
}
