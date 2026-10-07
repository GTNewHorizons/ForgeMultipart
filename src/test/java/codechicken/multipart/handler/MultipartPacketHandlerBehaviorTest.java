package codechicken.multipart.handler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;

import org.junit.jupiter.api.Test;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.ControlKeyModifer;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.TileCache;
import io.netty.buffer.ByteBuf;
import scala.MatchError;

class MultipartPacketHandlerBehaviorTest {

    @Test
    void reportsEveryMissingServerPartInOrder() {
        AtomicReference<S40PacketDisconnect> disconnect = new AtomicReference<>();
        PacketCustom packet = new PacketCustom("test", 1).writeInt(2).writeString("server:missing-one")
                .writeString("server:missing-two");
        try {
            MultipartCPH.handlePartRegistration(incoming(packet), clientHandler(disconnect));
        } finally {
            MultiPartRegistry.beforeServerStart();
        }

        S40PacketDisconnect disconnectPacket = disconnect.get();
        assertNotNull(disconnectPacket);
        ChatComponentTranslation reason = (ChatComponentTranslation) disconnectPacket.func_149165_c();
        assertEquals("multipart.missing", reason.getKey());
        assertArrayEquals(new Object[] { "server:missing-one, server:missing-two" }, reason.getFormatArgs());
    }

    @Test
    void turnsClientDesyncErrorsIntoAPlainDisconnectReason() throws Exception {
        TileCache.clear();
        EmptyWorldClient world = allocateInstance(EmptyWorldClient.class);
        world.isRemote = true;
        Minecraft minecraft = allocateInstance(Minecraft.class);
        minecraft.theWorld = world;
        AtomicReference<S40PacketDisconnect> disconnect = new AtomicReference<>();
        PacketCustom packet = new PacketCustom("test", 3).writeInt(4).writeInt(5).writeInt(6).writeByte(0);

        MultipartCPH.handlePacket(incoming(packet), minecraft, clientHandler(disconnect));

        S40PacketDisconnect disconnectPacket = disconnect.get();
        assertNotNull(disconnectPacket);
        ChatComponentText reason = (ChatComponentText) disconnectPacket.func_149165_c();
        String text = reason.getUnformattedText();
        assertTrue(text.startsWith("Client multipart @"));
        assertTrue(text.endsWith(" not found"));
        assertFalse(text.startsWith("DC: "));
    }

    @Test
    void keepsBothCompressedPacketTerminators() {
        PacketCustom description = incoming(new PacketCustom("test", 2).writeInt(-3).writeInt(7).writeShort(0));
        PacketCustom updates = incoming(
                new PacketCustom("test", 3).writeInt(16).writeInt(64).writeInt(32).writeByte(255)
                        .writeInt(Integer.MAX_VALUE));

        assertDoesNotThrow(() -> MultipartCPH.handleCompressedTileDesc(description, null));
        assertDoesNotThrow(() -> MultipartCPH.handleCompressedTileData(updates, null));
        assertFullyRead(description);
        assertFullyRead(updates);
    }

    @Test
    void rejectsUnknownPacketTypesLikeTheScalaMatches() {
        assertThrows(
                MatchError.class,
                () -> MultipartCPH.handlePacket(incoming(new PacketCustom("test", 4)), null, clientHandler(null)));
        assertThrows(
                MatchError.class,
                () -> MultipartSPH.handlePacket(incoming(new PacketCustom("test", 2)), null, null));
    }

    @Test
    void serverPacketOneUpdatesTheControlKeyState() {
        ControlKeyModifer.map().remove(null);
        try {
            MultipartSPH.handlePacket(incoming(new PacketCustom("test", 1).writeBoolean(true)), null, null);
            assertEquals(Boolean.TRUE, ControlKeyModifer.map().get(null));

            MultipartSPH$.MODULE$.handlePacket(incoming(new PacketCustom("test", 1).writeBoolean(false)), null, null);
            assertEquals(Boolean.FALSE, ControlKeyModifer.map().get(null));
        } finally {
            ControlKeyModifer.map().remove(null);
        }
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void tileStreamsAreCachedPerPositionAndStartWithTheCoordinate() throws Exception {
        EmptyWorldClient world = allocateInstance(EmptyWorldClient.class);
        world.isRemote = false;
        BlockCoord position = new BlockCoord(0x01020304, -2, 0x0A0B0C0D);
        scala.collection.mutable.Map updates = MultipartSPH$.MODULE$
                .codechicken$multipart$handler$MultipartSPH$$updateMap();
        updates.remove(world);
        try {
            MultipartSPH.MCByteStream stream = MultipartSPH.getTileStream(world, position);
            assertSame(stream, MultipartSPH$.MODULE$.getTileStream(world, position));
            stream.writeByte(0x7F);
            assertArrayEquals(new byte[] { 1, 2, 3, 4, -1, -1, -1, -2, 10, 11, 12, 13, 127 }, stream.getBytes());
        } finally {
            updates.remove(world);
        }
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void tileStreamsRejectClientWorldsBeforeCreatingState() throws Exception {
        EmptyWorldClient world = allocateInstance(EmptyWorldClient.class);
        world.isRemote = true;
        scala.collection.mutable.Map updates = MultipartSPH$.MODULE$
                .codechicken$multipart$handler$MultipartSPH$$updateMap();
        updates.remove(world);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> MultipartSPH.getTileStream(world, new BlockCoord(1, 2, 3)));

        assertEquals("Cannot use MultipartSPH on a client world", error.getMessage());
        assertFalse(updates.contains(world));
    }

    private static PacketCustom incoming(PacketCustom packet) {
        return new PacketCustom(packet.getByteBuf().copy());
    }

    private static void assertFullyRead(PacketCustom packet) {
        ByteBuf buffer = packet.getByteBuf();
        assertEquals(buffer.writerIndex(), buffer.readerIndex());
    }

    private static INetHandlerPlayClient clientHandler(AtomicReference<S40PacketDisconnect> disconnect) {
        return (INetHandlerPlayClient) Proxy.newProxyInstance(
                INetHandlerPlayClient.class.getClassLoader(),
                new Class<?>[] { INetHandlerPlayClient.class },
                (proxy, method, arguments) -> {
                    if ("handleDisconnect".equals(method.getName())) {
                        if (disconnect != null) {
                            disconnect.set((S40PacketDisconnect) arguments[0]);
                        }
                        return null;
                    }
                    throw new AssertionError("Unexpected client callback: " + method.getName());
                });
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocateInstance(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (T) unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }

    private static final class EmptyWorldClient extends WorldClient {

        private EmptyWorldClient() {
            super((NetHandlerPlayClient) null, (WorldSettings) null, 0, (EnumDifficulty) null, (Profiler) null);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected int func_152379_p() {
            return 0;
        }

        @Override
        public Entity getEntityByID(int id) {
            return null;
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            return null;
        }
    }
}
