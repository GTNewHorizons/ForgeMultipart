package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import org.junit.jupiter.api.Test;

import codechicken.lib.packet.PacketCustom;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.handler.MicroblockSPH;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import scala.Tuple2;

class MicroblockPacketHandlerFunctionalTest {

    @Test
    void sendsTheCompleteMaterialIdMapDuringTheServerHandshake() {
        MinecraftServer server = MinecraftServer.getServer();
        WorldServer world = server.worldServers[0];
        EntityPlayerMP player = FakePlayerFactory.getMinecraft(world);
        NetHandlerPlayServer previousHandler = player.playerNetServerHandler;

        RecordingNetHandler handler = new RecordingNetHandler(server, player);
        try {
            MicroblockSPH.handshakeRecieved(handler);
        } finally {
            player.playerNetServerHandler = previousHandler;
        }

        FMLProxyPacket sent = assertInstanceOf(FMLProxyPacket.class, handler.sent);
        assertEquals("ForgeMicroblock", sent.channel());

        PacketCustom payload = new PacketCustom(sent.payload().copy());
        assertEquals(1, payload.getType());
        Tuple2<String, MicroMaterialRegistry.IMicroMaterial>[] materials = MicroMaterialRegistry.getIdMap();
        assertEquals(materials.length, payload.readInt());
        for (Tuple2<String, MicroMaterialRegistry.IMicroMaterial> material : materials) {
            assertEquals(material._1(), payload.readString());
        }
        assertEquals(0, payload.getByteBuf().readableBytes());
    }

    private static final class RecordingNetHandler extends NetHandlerPlayServer {

        private Packet sent;

        private RecordingNetHandler(MinecraftServer server, EntityPlayerMP player) {
            super(server, new NetworkManager(false), player);
        }

        @Override
        public void sendPacket(Packet packet) {
            assertNotNull(packet);
            sent = packet;
        }
    }
}
