package codechicken.multipart.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.world.World;

import codechicken.lib.packet.PacketCustom;

public final class MultipartCPH {

    private MultipartCPH() {}

    public static MultipartMod$ channel() {
        return MultipartCPH$.MODULE$.channel();
    }

    public static String registryChannel() {
        return MultipartCPH$.MODULE$.registryChannel();
    }

    public static void handlePacket(PacketCustom packet, Minecraft minecraft, INetHandlerPlayClient netHandler) {
        MultipartCPH$.MODULE$.handlePacket(packet, minecraft, netHandler);
    }

    public static void handlePartRegistration(PacketCustom packet, INetHandlerPlayClient netHandler) {
        MultipartCPH$.MODULE$.handlePartRegistration(packet, netHandler);
    }

    public static void handleCompressedTileDesc(PacketCustom packet, World world) {
        MultipartCPH$.MODULE$.handleCompressedTileDesc(packet, world);
    }

    public static void handleCompressedTileData(PacketCustom packet, World world) {
        MultipartCPH$.MODULE$.handleCompressedTileData(packet, world);
    }
}
