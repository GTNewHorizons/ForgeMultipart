package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;

import net.minecraft.launchwrapper.LaunchClassLoader;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import codechicken.multipart.asm.ASMMixinCompiler$;
import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import codechicken.multipart.asm.ASMMixinCompiler.MixinInfo;
import scala.collection.JavaConversions;
import scala.collection.mutable.Map;

class ASMMixinCompilerStartupFunctionalTest {

    private static final ASMMixinCompiler$ COMPILER = ASMMixinCompiler$.MODULE$;
    private static final String PREFIX = "codechicken/multipart/test/bootstrap/";

    @Test
    void bindsTheCompilerToItsActualForgeLaunchClassLoader() {
        assertSame(ASMMixinCompiler$.class.getClassLoader(), COMPILER.cl());
        assertInstanceOf(LaunchClassLoader.class, COMPILER.cl());
    }

    @Test
    void resolvesAndOpensTheExactJdkAndLaunchWrapperMembers() {
        Method define = COMPILER.m_defineClass();
        assertEquals(ClassLoader.class, define.getDeclaringClass());
        assertEquals("defineClass", define.getName());
        assertArrayEquals(new Class<?>[] { byte[].class, int.class, int.class }, define.getParameterTypes());
        assertEquals(Class.class, define.getReturnType());
        assertTrue(Modifier.isFinal(define.getModifiers()));
        assertTrue(define.isAccessible());

        Method transform = COMPILER.m_runTransformers();
        assertEquals(LaunchClassLoader.class, transform.getDeclaringClass());
        assertEquals("runTransformers", transform.getName());
        assertArrayEquals(new Class<?>[] { String.class, String.class, byte[].class }, transform.getParameterTypes());
        assertEquals(byte[].class, transform.getReturnType());
        assertTrue(Modifier.isPrivate(transform.getModifiers()));
        assertTrue(transform.isAccessible());

        Field exclusions = COMPILER.f_transformerExceptions();
        assertEquals(LaunchClassLoader.class, exclusions.getDeclaringClass());
        assertEquals("transformerExceptions", exclusions.getName());
        assertEquals(Set.class, exclusions.getType());
        assertTrue(Modifier.isPrivate(exclusions.getModifiers()));
        assertTrue(exclusions.isAccessible());
    }

    @Test
    void openedDefineMethodDefinesExecutableBytesOnAnIndependentLoader() throws Exception {
        byte[] bytes = executable(PREFIX + "Defined", 73);
        try (LaunchClassLoader loader = new LaunchClassLoader(new URL[0])) {
            Class<?> defined = (Class<?>) COMPILER.m_defineClass().invoke(loader, bytes, 0, bytes.length);
            assertSame(loader, defined.getClassLoader());
            assertEquals(PREFIX.replace('/', '.') + "Defined", defined.getName());
            assertEquals(73, defined.getMethod("value").invoke(null));
        }
    }

    @Test
    void openedTransformerMethodUsesTheReceiverLoaderAndPreservesBytesWithNoTransformers() throws Exception {
        byte[] bytes = executable(PREFIX + "Transformed", 29);
        try (LaunchClassLoader loader = new LaunchClassLoader(new URL[0])) {
            byte[] transformed = (byte[]) COMPILER.m_runTransformers().invoke(
                    loader,
                    PREFIX.replace('/', '.') + "Transformed",
                    PREFIX.replace('/', '.') + "Transformed",
                    bytes);
            assertSame(bytes, transformed);
            assertEquals(PREFIX + "Transformed", new ClassReader(transformed).getClassName());
            assertTrue(loader.getTransformers().isEmpty());
        }
    }

    @Test
    void openedTransformerExceptionFieldIsTheLiveSetUsedByTheCompilerLoader() throws Exception {
        Field exclusions = COMPILER.f_transformerExceptions();
        @SuppressWarnings("unchecked")
        Set<String> value = (Set<String>) exclusions.get(COMPILER.cl());
        Field direct = LaunchClassLoader.class.getDeclaredField("transformerExceptions");
        direct.setAccessible(true);
        assertSame(direct.get(COMPILER.cl()), value);
        String marker = PREFIX.replace('/', '.') + "excluded";
        boolean present = value.contains(marker);
        try {
            value.add(marker);
            assertTrue(((Set<?>) direct.get(COMPILER.cl())).contains(marker));
        } finally {
            if (!present) value.remove(marker);
        }
    }

    @Test
    void startupMapStorageUsesIndependentMutableHashMaps() throws Exception {
        Map<String, byte[]> bytes = map("traitByteMap");
        Map<String, MixinInfo> mixins = map("mixinMap");
        Map<String, ClassInfo> info = map("infoCache");
        assertEquals("scala.collection.mutable.HashMap", bytes.getClass().getName());
        assertEquals(bytes.getClass(), mixins.getClass());
        assertEquals(bytes.getClass(), info.getClass());
        assertNotSame(bytes, mixins);
        assertNotSame(bytes, info);
        assertNotSame(mixins, info);

        java.util.Map<String, byte[]> oldBytes = new HashMap<>(JavaConversions.mapAsJavaMap(bytes));
        java.util.Map<String, MixinInfo> oldMixins = new HashMap<>(JavaConversions.mapAsJavaMap(mixins));
        java.util.Map<String, ClassInfo> oldInfo = new HashMap<>(JavaConversions.mapAsJavaMap(info));
        try {
            String key = PREFIX + "independent";
            bytes.put(key, new byte[] { 1 });
            assertFalse(mixins.contains(key));
            assertFalse(info.contains(key));
            mixins.put(key, null);
            assertFalse(info.contains(key));
        } finally {
            bytes.clear();
            oldBytes.forEach(bytes::put);
            mixins.clear();
            oldMixins.forEach(mixins::put);
            info.clear();
            oldInfo.forEach(info::put);
        }
    }

    @Test
    void sanityCheckerBytesAreAvailableThroughTheInitializedCompilerPath() throws Exception {
        byte[] bytes = COMPILER.getBytes("cpw/mods/fml/common/asm/FMLSanityChecker");
        assertNotNull(bytes);
        ClassReader reader = new ClassReader(bytes);
        assertEquals("cpw/mods/fml/common/asm/FMLSanityChecker", reader.getClassName());
        assertEquals("java/lang/Object", reader.getSuperName());
        assertTrue(reader.getItemCount() > 1);
    }

    private static byte[] executable(String name, int value) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V1_6, ACC_PUBLIC, name, null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "value", "()I", null, null);
        method.visitLdcInsn(value);
        method.visitInsn(IRETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> map(String name) throws Exception {
        Field field = ASMMixinCompiler$.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<String, T>) field.get(COMPILER);
    }
}
