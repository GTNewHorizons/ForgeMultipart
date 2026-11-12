package codechicken.microblock.handler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.packet.PacketCustom.IClientPacketHandler;
import codechicken.lib.packet.PacketCustom.IHandshakeHandler;
import codechicken.lib.packet.PacketCustom.IServerPacketHandler;

class MicroblockPacketHandlerCharacterizationTest {

    private static final Set<String> CLIENT_METHODS = new TreeSet<>(
            Arrays.asList(
                    "handleMaterialRegistration(Lcodechicken/lib/packet/PacketCustom;Lnet/minecraft/network/play/INetHandlerPlayClient;)V",
                    "handlePacket(Lcodechicken/lib/packet/PacketCustom;Lnet/minecraft/client/Minecraft;Lnet/minecraft/network/play/INetHandlerPlayClient;)V"));
    private static final Set<String> SERVER_METHODS = new TreeSet<>(
            Arrays.asList(
                    "handlePacket(Lcodechicken/lib/packet/PacketCustom;Lnet/minecraft/entity/player/EntityPlayerMP;Lnet/minecraft/network/play/INetHandlerPlayServer;)V",
                    "handshakeRecieved(Lnet/minecraft/network/NetHandlerPlayServer;)V"));

    @Test
    void keepsTheSharedPacketHandlerBase() throws Exception {
        assertTrue(Modifier.isPublic(MicroblockPH.class.getModifiers()));
        assertFalse(Modifier.isFinal(MicroblockPH.class.getModifiers()));
        assertArrayEquals(new Class<?>[0], MicroblockPH.class.getInterfaces());
        assertEquals(
                new TreeSet<>(Arrays.asList("registryChannel()Ljava/lang/String;")),
                publicMethodSignatures(MicroblockPH.class));

        Field channel = MicroblockPH.class.getDeclaredField("registryChannel");
        assertSame(String.class, channel.getType());
        assertTrue(Modifier.isPrivate(channel.getModifiers()));
        assertTrue(Modifier.isFinal(channel.getModifiers()));
        assertEquals("ForgeMicroblock", new MicroblockPH().registryChannel());
    }

    @Test
    void keepsTheClientFacadeAndCompanion() throws Exception {
        assertFacade(MicroblockCPH.class, withRegistryChannel(CLIENT_METHODS));
        assertCompanion(MicroblockCPH$.class, IClientPacketHandler.class, CLIENT_METHODS);
        assertSame(MicroblockCPH$.class, MicroblockCPH$.class.getField("MODULE$").getType());
        assertSame(MicroblockCPH$.MODULE$, MicroblockCPH$.class.getField("MODULE$").get(null));
        assertEquals("ForgeMicroblock", MicroblockCPH.registryChannel());
    }

    @Test
    void keepsTheServerFacadeAndCompanion() throws Exception {
        assertFacade(MicroblockSPH.class, withRegistryChannel(SERVER_METHODS));
        assertCompanion(
                MicroblockSPH$.class,
                new Class<?>[] { IServerPacketHandler.class, IHandshakeHandler.class },
                SERVER_METHODS);
        assertSame(MicroblockSPH$.class, MicroblockSPH$.class.getField("MODULE$").getType());
        assertSame(MicroblockSPH$.MODULE$, MicroblockSPH$.class.getField("MODULE$").get(null));
        assertEquals("ForgeMicroblock", MicroblockSPH.registryChannel());
    }

    private static void assertFacade(Class<?> type, Set<String> methods) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertArrayEquals(new Class<?>[0], type.getInterfaces());
        assertEquals(methods, publicMethodSignatures(type));
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                assertTrue(Modifier.isStatic(method.getModifiers()), method.toString());
            }
        }
    }

    private static void assertCompanion(Class<?> type, Class<?> packetInterface, Set<String> methods) throws Exception {
        assertCompanion(type, new Class<?>[] { packetInterface }, methods);
    }

    private static void assertCompanion(Class<?> type, Class<?>[] packetInterfaces, Set<String> methods)
            throws Exception {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertSame(MicroblockPH.class, type.getSuperclass());
        assertArrayEquals(packetInterfaces, type.getInterfaces());
        assertEquals(methods, publicMethodSignatures(type));

        Field module = type.getField("MODULE$");
        assertTrue(Modifier.isPublic(module.getModifiers()));
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                assertFalse(Modifier.isStatic(method.getModifiers()), method.toString());
            }
        }
    }

    private static Set<String> withRegistryChannel(Set<String> methods) {
        Set<String> result = new TreeSet<>(methods);
        result.add("registryChannel()Ljava/lang/String;");
        return result;
    }

    private static Set<String> publicMethodSignatures(Class<?> type) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return signatures;
    }
}
