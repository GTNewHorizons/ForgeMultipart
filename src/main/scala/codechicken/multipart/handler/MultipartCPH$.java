package codechicken.multipart.handler;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustom.IClientPacketHandler;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.TileMultipart;
import scala.MatchError;

public final class MultipartCPH$ extends MultipartPH implements IClientPacketHandler {

    public static final MultipartCPH$ MODULE$ = new MultipartCPH$();

    private MultipartCPH$() {}

    @Override
    public void handlePacket(PacketCustom packet, Minecraft minecraft, INetHandlerPlayClient netHandler) {
        try {
            int type = packet.getType();
            switch (type) {
                case 1:
                    handlePartRegistration(packet, netHandler);
                    return;
                case 2:
                    handleCompressedTileDesc(packet, minecraft.theWorld);
                    return;
                case 3:
                    handleCompressedTileData(packet, minecraft.theWorld);
                    return;
                default:
                    throw new MatchError(type);
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("DC: ")) {
                netHandler
                        .handleDisconnect(new S40PacketDisconnect(new ChatComponentText(e.getMessage().substring(4))));
                return;
            }
            throw e;
        }
    }

    public void handlePartRegistration(PacketCustom packet, INetHandlerPlayClient netHandler) {
        List<String> missing = MultiPartRegistry.readIDMap(packet);
        if (!missing.isEmpty()) {
            netHandler.handleDisconnect(
                    new S40PacketDisconnect(
                            new ChatComponentTranslation("multipart.missing", String.join(", ", missing))));
        }
    }

    public void handleCompressedTileDesc(PacketCustom packet, World world) {
        ChunkCoordIntPair chunk = new ChunkCoordIntPair(packet.readInt(), packet.readInt());
        int count = packet.readUShort();
        for (int i = 0; i < count; i++) {
            TileMultipart.handleDescPacket(
                    world,
                    new BlockCoord(
                            packet.readByte() + (chunk.chunkXPos << 4),
                            packet.readInt(),
                            packet.readByte() + (chunk.chunkZPos << 4)),
                    packet);
        }
    }

    public void handleCompressedTileData(PacketCustom packet, World world) {
        int x = packet.readInt();
        while (x != Integer.MAX_VALUE) {
            BlockCoord position = new BlockCoord(x, packet.readInt(), packet.readInt());
            int part = packet.readUByte();
            while (part < 255) {
                TileMultipart.handlePacket(position, world, part, packet);
                part = packet.readUByte();
            }
            x = packet.readInt();
        }
    }
}
