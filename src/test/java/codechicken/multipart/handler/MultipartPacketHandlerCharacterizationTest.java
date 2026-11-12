package codechicken.multipart.handler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.data.MCDataOutputWrapper;
import codechicken.lib.packet.PacketCustom.IClientPacketHandler;
import codechicken.lib.packet.PacketCustom.IHandshakeHandler;
import codechicken.lib.packet.PacketCustom.IServerPacketHandler;
import scala.collection.mutable.HashMap;
import scala.collection.mutable.Map;

class MultipartPacketHandlerCharacterizationTest {

    private static final Set<String> CLIENT_METHODS = new TreeSet<>(
            Arrays.asList(
                    "handleCompressedTileData(Lcodechicken/lib/packet/PacketCustom;Lnet/minecraft/world/World;)V",
                    "handleCompressedTileDesc(Lcodechicken/lib/packet/PacketCustom;Lnet/minecraft/world/World;)V",
                    "handlePartRegistration(Lcodechicken/lib/packet/PacketCustom;Lnet/minecraft/network/play/INetHandlerPlayClient;)V",
                    "handlePacket(Lcodechicken/lib/packet/PacketCustom;Lnet/minecraft/client/Minecraft;Lnet/minecraft/network/play/INetHandlerPlayClient;)V"));
    private static final Set<String> SERVER_METHODS = new TreeSet<>(
            Arrays.asList(
                    "codechicken$multipart$handler$MultipartSPH$$chunkWatchers()Lscala/collection/mutable/HashMap;",
                    "codechicken$multipart$handler$MultipartSPH$$newWatchers()Lscala/collection/mutable/Map;",
                    "codechicken$multipart$handler$MultipartSPH$$updateMap()Lscala/collection/mutable/Map;",
                    "getDescPacket(Lnet/minecraft/world/chunk/Chunk;Ljava/util/Iterator;)Lcodechicken/lib/packet/PacketCustom;",
                    "getTileStream(Lnet/minecraft/world/World;Lcodechicken/lib/vec/BlockCoord;)Lcodechicken/multipart/handler/MultipartSPH$MCByteStream;",
                    "handlePacket(Lcodechicken/lib/packet/PacketCustom;Lnet/minecraft/entity/player/EntityPlayerMP;Lnet/minecraft/network/play/INetHandlerPlayServer;)V",
                    "handshakeRecieved(Lnet/minecraft/network/NetHandlerPlayServer;)V",
                    "onChunkUnWatch(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/ChunkCoordIntPair;)V",
                    "onChunkWatch(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/ChunkCoordIntPair;)V",
                    "onTickEnd(Lscala/collection/Seq;)V",
                    "onWorldUnload(Lnet/minecraft/world/World;)V"));

    @Test
    void keepsTheSharedPacketHandlerBase() throws Exception {
        assertTrue(Modifier.isPublic(MultipartPH.class.getModifiers()));
        assertFalse(Modifier.isFinal(MultipartPH.class.getModifiers()));
        assertArrayEquals(new Class<?>[0], MultipartPH.class.getInterfaces());
        assertEquals(
                new TreeSet<>(
                        Arrays.asList(
                                "channel()Lcodechicken/multipart/handler/MultipartMod$;",
                                "registryChannel()Ljava/lang/String;")),
                publicMethodSignatures(MultipartPH.class));

        assertPrivateFinalField(MultipartPH.class, "channel", MultipartMod$.class);
        assertPrivateFinalField(MultipartPH.class, "registryChannel", String.class);
        MultipartPH handler = new MultipartPH();
        assertSame(MultipartMod$.MODULE$, handler.channel());
        assertEquals("ForgeMultipart", handler.registryChannel());
    }

    @Test
    void keepsTheClientFacadeAndCompanion() throws Exception {
        assertFacade(MultipartCPH.class, withSharedChannels(CLIENT_METHODS));
        assertCompanion(MultipartCPH$.class, new Class<?>[] { IClientPacketHandler.class }, CLIENT_METHODS);
        assertSame(MultipartCPH$.class, MultipartCPH$.class.getField("MODULE$").getType());
        assertSame(MultipartCPH$.MODULE$, MultipartCPH$.class.getField("MODULE$").get(null));
        assertSame(MultipartMod$.MODULE$, MultipartCPH.channel());
        assertEquals("ForgeMultipart", MultipartCPH.registryChannel());
    }

    @Test
    void keepsTheServerFacadeCompanionAndStateAccessors() throws Exception {
        Set<String> facadeMethods = withSharedChannels(SERVER_METHODS);
        facadeMethods.remove(
                "codechicken$multipart$handler$MultipartSPH$$chunkWatchers()Lscala/collection/mutable/HashMap;");
        facadeMethods.remove("codechicken$multipart$handler$MultipartSPH$$newWatchers()Lscala/collection/mutable/Map;");
        facadeMethods.remove("codechicken$multipart$handler$MultipartSPH$$updateMap()Lscala/collection/mutable/Map;");
        assertFacade(MultipartSPH.class, facadeMethods);
        assertCompanion(
                MultipartSPH$.class,
                new Class<?>[] { IServerPacketHandler.class, IHandshakeHandler.class },
                SERVER_METHODS);
        assertSame(MultipartSPH$.class, MultipartSPH$.class.getField("MODULE$").getType());
        assertSame(MultipartSPH$.MODULE$, MultipartSPH$.class.getField("MODULE$").get(null));

        assertPrivateFinalField(
                MultipartSPH$.class,
                "codechicken$multipart$handler$MultipartSPH$$updateMap",
                Map.class);
        assertPrivateFinalField(
                MultipartSPH$.class,
                "codechicken$multipart$handler$MultipartSPH$$chunkWatchers",
                HashMap.class);
        assertPrivateFinalField(
                MultipartSPH$.class,
                "codechicken$multipart$handler$MultipartSPH$$newWatchers",
                Map.class);
        assertSame(MultipartMod$.MODULE$, MultipartSPH.channel());
        assertEquals("ForgeMultipart", MultipartSPH.registryChannel());
    }

    @Test
    void keepsTheNestedByteStreamShape() throws Exception {
        Class<?> type = MultipartSPH.MCByteStream.class;
        assertSame(MultipartSPH.class, type.getDeclaringClass());
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isStatic(type.getModifiers()));
        assertFalse(Modifier.isFinal(type.getModifiers()));
        assertSame(MCDataOutputWrapper.class, type.getSuperclass());
        assertArrayEquals(new Class<?>[0], type.getInterfaces());
        assertEquals(new TreeSet<>(Arrays.asList("getBytes()[B")), publicMethodSignatures(type));
        assertPrivateFinalField(type, "bout", ByteArrayOutputStream.class);
        assertSame(type, type.getConstructor(ByteArrayOutputStream.class).getDeclaringClass());
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

    private static void assertCompanion(Class<?> type, Class<?>[] packetInterfaces, Set<String> methods)
            throws Exception {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertSame(MultipartPH.class, type.getSuperclass());
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

    private static void assertPrivateFinalField(Class<?> owner, String name, Class<?> type) throws Exception {
        Field field = owner.getDeclaredField(name);
        assertSame(type, field.getType());
        assertTrue(Modifier.isPrivate(field.getModifiers()));
        assertTrue(Modifier.isFinal(field.getModifiers()));
    }

    private static Set<String> withSharedChannels(Set<String> methods) {
        Set<String> result = new TreeSet<>(methods);
        result.add("channel()Lcodechicken/multipart/handler/MultipartMod$;");
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
