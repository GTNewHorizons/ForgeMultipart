package codechicken.microblock.handler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.ChatComponentTranslation;

import org.junit.jupiter.api.Test;

import codechicken.lib.packet.PacketCustom;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.MissingMicroMaterial;
import codechicken.microblock.MissingMicroMaterial$;
import io.netty.buffer.Unpooled;
import scala.MatchError;

class MicroblockPacketHandlerBehaviorTest {

    @Test
    void skipsTheSharedRegistryPacketInSinglePlayer() throws Exception {
        PacketCustom packetWithoutPayload = new PacketCustom(Unpooled.buffer().writeByte(1));

        assertDoesNotThrow(
                () -> MicroblockCPH.handlePacket(packetWithoutPayload, minecraft(true), clientHandler(null)));
    }

    @Test
    void reportsEveryMissingServerMaterialInOrder() throws Exception {
        if (MicroMaterialRegistry.getMaterial(MissingMicroMaterial.key()) == null) {
            MicroMaterialRegistry.registerMaterial(MissingMicroMaterial$.MODULE$, MissingMicroMaterial.key());
        }

        AtomicReference<S40PacketDisconnect> disconnect = new AtomicReference<>();
        PacketCustom packet = incomingPacket(1, "server:missing-one", "server:missing-two");
        try {
            MicroblockCPH.handleMaterialRegistration(packet, clientHandler(disconnect));
        } finally {
            MicroMaterialRegistry.setupIDMap();
        }

        S40PacketDisconnect disconnectPacket = disconnect.get();
        assertNotNull(disconnectPacket);
        ChatComponentTranslation reason = (ChatComponentTranslation) disconnectPacket.func_149165_c();
        assertEquals("microblock.missing", reason.getKey());
        assertArrayEquals(new Object[] { "server:missing-one, server:missing-two" }, reason.getFormatArgs());
    }

    @Test
    void rejectsUnknownClientPacketTypesLikeTheScalaMatch() throws Exception {
        PacketCustom packet = new PacketCustom(Unpooled.buffer().writeByte(2));

        assertThrows(MatchError.class, () -> MicroblockCPH.handlePacket(packet, null, clientHandler(null)));
    }

    @Test
    void leavesTheServerPacketCallbackAsANoOp() {
        assertDoesNotThrow(() -> MicroblockSPH.handlePacket(null, null, null));
        assertDoesNotThrow(() -> MicroblockSPH$.MODULE$.handlePacket(null, null, null));
    }

    private static PacketCustom incomingPacket(int type, String... materialNames) {
        PacketCustom packet = new PacketCustom("test", type).writeInt(materialNames.length);
        for (String materialName : materialNames) {
            packet.writeString(materialName);
        }
        return new PacketCustom(packet.getByteBuf().copy());
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

    private static Minecraft minecraft(boolean singlePlayer) throws Exception {
        Minecraft minecraft = (Minecraft) allocateInstance(Minecraft.class);
        if (singlePlayer) {
            Field running = Minecraft.class.getDeclaredField("integratedServerIsRunning");
            running.setAccessible(true);
            running.setBoolean(minecraft, true);

            Field server = Minecraft.class.getDeclaredField("theIntegratedServer");
            server.setAccessible(true);
            server.set(minecraft, allocateInstance(server.getType()));
        }
        return minecraft;
    }

    private static Object allocateInstance(Class<?> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }
}
