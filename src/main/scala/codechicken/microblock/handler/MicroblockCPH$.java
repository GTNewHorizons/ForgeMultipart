package codechicken.microblock.handler;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.ChatComponentTranslation;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustom.IClientPacketHandler;
import codechicken.microblock.MicroMaterialRegistry;
import scala.MatchError;

public final class MicroblockCPH$ extends MicroblockPH implements IClientPacketHandler {

    public static final MicroblockCPH$ MODULE$ = new MicroblockCPH$();

    private MicroblockCPH$() {}

    @Override
    public void handlePacket(PacketCustom packet, Minecraft minecraft, INetHandlerPlayClient netHandler) {
        int type = packet.getType();
        if (type == 1) {
            if (!minecraft.isSingleplayer()) {
                handleMaterialRegistration(packet, netHandler);
            }
            return;
        }
        throw new MatchError(type);
    }

    public void handleMaterialRegistration(PacketCustom packet, INetHandlerPlayClient netHandler) {
        List<String> missing = MicroMaterialRegistry.readIDMap(packet);
        if (!missing.isEmpty()) {
            netHandler.handleDisconnect(
                    new S40PacketDisconnect(
                            new ChatComponentTranslation("microblock.missing", String.join(", ", missing))));
        }
    }
}
