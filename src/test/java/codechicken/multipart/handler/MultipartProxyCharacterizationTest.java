package codechicken.multipart.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.world.ChunkCoordIntPair;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.config.ConfigFile;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.BlockMultipart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

class MultipartProxyCharacterizationTest {

    private static final Set<String> SERVER_METHODS = new TreeSet<>(
            Arrays.asList(
                    "block()Lcodechicken/multipart/BlockMultipart;",
                    "block_$eq(Lcodechicken/multipart/BlockMultipart;)V",
                    "config()Lcodechicken/lib/config/ConfigFile;",
                    "config_$eq(Lcodechicken/lib/config/ConfigFile;)V",
                    "init()V",
                    "logger()Lorg/apache/logging/log4j/Logger;",
                    "logger_$eq(Lorg/apache/logging/log4j/Logger;)V",
                    "onTileClassBuilt(Ljava/lang/Class;)V",
                    "postInit()V",
                    "preInit(Ljava/io/File;Lorg/apache/logging/log4j/Logger;)V"));
    private static final Set<String> CLIENT_METHODS = new TreeSet<>(
            Arrays.asList("onTileClassBuilt(Ljava/lang/Class;)V", "postInit()V"));
    private static final Set<String> FACADE_METHODS = new TreeSet<>();
    private static final Set<String> COMPANION_METHODS = new TreeSet<>(
            Arrays.asList(
                    "indexInChunk(Lcodechicken/lib/vec/BlockCoord;)I",
                    "indexInChunk(Lnet/minecraft/world/ChunkCoordIntPair;I)Lcodechicken/lib/vec/BlockCoord;"));

    static {
        FACADE_METHODS.addAll(SERVER_METHODS);
        FACADE_METHODS.addAll(COMPANION_METHODS);
    }

    @Test
    void keepsTheFourTypeHierarchyAndPublicSurfaces() throws Exception {
        assertEquals(Object.class, MultipartProxy_serverImpl.class.getSuperclass());
        assertEquals(MultipartProxy_serverImpl.class, MultipartProxy_clientImpl.class.getSuperclass());
        assertEquals(Object.class, MultipartProxy.class.getSuperclass());
        assertEquals(MultipartProxy_clientImpl.class, MultipartProxy$.class.getSuperclass());
        assertFalse(Modifier.isFinal(MultipartProxy_serverImpl.class.getModifiers()));
        assertFalse(Modifier.isFinal(MultipartProxy_clientImpl.class.getModifiers()));
        assertTrue(Modifier.isFinal(MultipartProxy.class.getModifiers()));
        assertTrue(Modifier.isFinal(MultipartProxy$.class.getModifiers()));

        assertEquals(SERVER_METHODS, publicDeclaredMethodSignatures(MultipartProxy_serverImpl.class));
        assertEquals(CLIENT_METHODS, publicDeclaredMethodSignatures(MultipartProxy_clientImpl.class));
        assertEquals(FACADE_METHODS, publicDeclaredMethodSignatures(MultipartProxy.class));
        assertEquals(COMPANION_METHODS, publicDeclaredMethodSignatures(MultipartProxy$.class));

        for (Method method : MultipartProxy.class.getDeclaredMethods()) {
            assertTrue(Modifier.isStatic(method.getModifiers()), method.toString());
        }
        for (Method method : MultipartProxy$.class.getDeclaredMethods()) {
            assertFalse(Modifier.isStatic(method.getModifiers()), method.toString());
        }

        assertEquals(3, MultipartProxy_serverImpl.class.getDeclaredFields().length);
        assertPrivateField(MultipartProxy_serverImpl.class, "block", BlockMultipart.class);
        assertPrivateField(MultipartProxy_serverImpl.class, "config", ConfigFile.class);
        assertPrivateField(MultipartProxy_serverImpl.class, "logger", Logger.class);
        assertEquals(0, MultipartProxy_clientImpl.class.getDeclaredFields().length);
        assertEquals(0, MultipartProxy.class.getDeclaredFields().length);

        Field module = MultipartProxy$.class.getField("MODULE$");
        assertSame(MultipartProxy$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(MultipartProxy$.MODULE$, module.get(null));
    }

    @Test
    void keepsOnlyTheClientOverridesAndTheirStaticForwardersSideOnly() {
        assertSideOnlyMethods(MultipartProxy_serverImpl.class, new TreeSet<String>());
        assertSideOnlyMethods(MultipartProxy_clientImpl.class, CLIENT_METHODS);
        assertSideOnlyMethods(MultipartProxy.class, CLIENT_METHODS);
        assertSideOnlyMethods(MultipartProxy$.class, new TreeSet<String>());

        assertNull(MultipartProxy_serverImpl.class.getAnnotation(SideOnly.class));
        assertNull(MultipartProxy_clientImpl.class.getAnnotation(SideOnly.class));
        assertNull(MultipartProxy.class.getAnnotation(SideOnly.class));
        assertNull(MultipartProxy$.class.getAnnotation(SideOnly.class));
    }

    @Test
    void facadeAndCompanionShareMutableSingletonState() {
        Logger original = MultipartProxy.logger();
        Logger marker = LogManager.getLogger("MultipartProxyCharacterizationTest.marker");
        try {
            MultipartProxy.logger_$eq(marker);
            assertSame(marker, MultipartProxy.logger());
            assertSame(marker, MultipartProxy$.MODULE$.logger());

            MultipartProxy$.MODULE$.logger_$eq(original);
            assertSame(original, MultipartProxy.logger());
        } finally {
            MultipartProxy$.MODULE$.logger_$eq(original);
        }

        MultipartProxy_serverImpl independent = new MultipartProxy_serverImpl();
        independent.logger_$eq(marker);
        assertSame(marker, independent.logger());
        assertSame(original, MultipartProxy.logger());
    }

    @Test
    void chunkIndicesKeepTheirExactLocalCoordinatePacking() {
        ChunkCoordIntPair chunk = new ChunkCoordIntPair(-2, 3);
        int index = 0xABCD;

        BlockCoord staticPosition = MultipartProxy.indexInChunk(chunk, index);
        BlockCoord companionPosition = MultipartProxy$.MODULE$.indexInChunk(chunk, index);
        assertCoordinates(staticPosition, -19, 0xAB, 60);
        assertCoordinates(companionPosition, -19, 0xAB, 60);
        assertEquals(index, MultipartProxy.indexInChunk(staticPosition));
        assertEquals(index, MultipartProxy$.MODULE$.indexInChunk(companionPosition));

        BlockCoord signedShort = MultipartProxy.indexInChunk(chunk, -1);
        assertCoordinates(signedShort, -17, 255, 63);
        assertEquals(0xFFFF, MultipartProxy.indexInChunk(signedShort));
    }

    private static void assertPrivateField(Class<?> owner, String name, Class<?> type) throws Exception {
        Field field = owner.getDeclaredField(name);
        assertSame(type, field.getType());
        assertTrue(Modifier.isPrivate(field.getModifiers()));
    }

    private static void assertSideOnlyMethods(Class<?> type, Set<String> expected) {
        Set<String> actual = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            SideOnly annotation = method.getAnnotation(SideOnly.class);
            if (annotation != null) {
                assertEquals(Side.CLIENT, annotation.value(), method.toString());
                actual.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        assertEquals(expected, actual);
    }

    private static void assertCoordinates(BlockCoord position, int x, int y, int z) {
        assertEquals(x, position.x);
        assertEquals(y, position.y);
        assertEquals(z, position.z);
    }

    private static Set<String> publicDeclaredMethodSignatures(Class<?> type) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return signatures;
    }
}
