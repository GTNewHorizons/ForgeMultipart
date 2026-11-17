package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;

import com.google.common.collect.HashBiMap;

import codechicken.lib.asm.ObfMapping;
import codechicken.multipart.asm.ASMMixinCompiler;
import codechicken.multipart.asm.ASMMixinCompiler$;
import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import codechicken.multipart.asm.DebugPrinter$;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import scala.collection.JavaConversions;
import scala.collection.mutable.Map;

class ClassBytesFunctionalTest {

    private static final String NAME = "codechicken/multipart/test/bytes/Probe";
    private static final String DOTTED = NAME.replace('/', '.');

    @Test
    void normalizesLoaderNamesAndRunsTheTransformerChainOnEveryDirectLookup() throws Exception {
        try (State state = new State()) {
            byte[] raw = { 1 }, middle = { 2 }, result = { 3 };
            state.loader.bytes = raw;
            state.bytes.put(NAME, new byte[] { 4 });
            List<String> calls = new ArrayList<>();
            state.transformers((name, transformed, input) -> {
                calls.add(name + ":" + transformed);
                assertSame(raw, input);
                return middle;
            }, (name, transformed, input) -> {
                assertSame(middle, input);
                return result;
            });
            assertSame(result, state.compiler.getBytes(NAME));
            assertSame(result, ASMMixinCompiler.getBytes(DOTTED));
            assertEquals(Arrays.asList(DOTTED, DOTTED), state.loader.names);
            assertEquals(Arrays.asList(DOTTED + ":" + DOTTED, DOTTED + ":" + DOTTED), calls);
            assertArrayEquals(new byte[] { 4 }, state.bytes.apply(NAME));
        }
    }

    @Test
    void objectAndMissingBytesSkipExclusionsAndNullNamesFailBeforeLoading() throws Exception {
        try (State state = new State()) {
            state.setExclusions(null);
            assertNull(state.compiler.getBytes("java/lang/Object"));
            assertNull(state.compiler.getBytes("java.lang.Object"));
            assertThrows(NullPointerException.class, () -> state.compiler.getBytes(null));
            assertTrue(state.loader.names.isEmpty());
            assertNull(state.compiler.getBytes(NAME));
            assertEquals(Arrays.asList(DOTTED), state.loader.names);
            state.loader.bytes = new byte[] { 1 };
            assertThrows(NullPointerException.class, () -> state.compiler.getBytes(NAME));
            assertEquals(Arrays.asList(DOTTED, DOTTED), state.loader.names);
        }
    }

    @Test
    void exclusionsUseDottedPrefixesAndStopAtTheFirstMatch() throws Exception {
        try (State state = new State()) {
            state.loader.bytes = new byte[] { 1 };
            state.transformers((name, transformed, bytes) -> {
                fail("Excluded bytes must not reach transformers");
                return bytes;
            });
            state.exclusions("unrelated", DOTTED.substring(0, DOTTED.length() - 1), null);
            assertSame(state.loader.bytes, state.compiler.getBytes(NAME));
            state.exclusions("", null);
            assertSame(state.loader.bytes, state.compiler.getBytes(NAME));
            state.exclusions(NAME, null);
            assertThrows(NullPointerException.class, () -> state.compiler.getBytes(NAME));
        }
    }

    @Test
    void preservesNullTransformerResultsAndReflectiveExceptionWrapping() throws Exception {
        try (State state = new State()) {
            state.loader.bytes = new byte[] { 1 };
            state.transformers((name, transformed, bytes) -> null);
            assertNull(state.compiler.getBytes(NAME));
            Error failure = new LinkageError("transformer probe");
            state.transformers((name, transformed, bytes) -> { throw failure; });
            InvocationTargetException thrown = assertThrows(
                    InvocationTargetException.class,
                    () -> state.compiler.getBytes(NAME));
            assertSame(failure, thrown.getCause());
            assertFalse(state.bytes.contains(NAME));
        }
    }

    @Test
    void remapsTheOriginalInputAndPassesDeobfuscatedThenObfuscatedNamesToTransformers() throws Exception {
        try (State state = new State()) {
            Field mappings = field(FMLDeobfuscatingRemapper.class, "classNameBiMap");
            Object previous = mappings.get(FMLDeobfuscatingRemapper.INSTANCE);
            Field obfuscated = writableStaticFinal(ObfMapping.class, "obfuscated");
            boolean oldObfuscated = obfuscated.getBoolean(null);
            HashBiMap<String, String> names = HashBiMap.create();
            names.put("obf/Probe", NAME);
            try {
                mappings.set(FMLDeobfuscatingRemapper.INSTANCE, names);
                obfuscated.setBoolean(null, true);
                byte[] raw = { 1 }, result = { 2 };
                state.loader.bytes = raw;
                List<String> calls = new ArrayList<>();
                state.transformers((name, transformed, bytes) -> {
                    assertSame(raw, bytes);
                    calls.add(name + ":" + transformed);
                    return result;
                });
                assertSame(result, state.compiler.getBytes(NAME));
                assertSame(result, state.compiler.getBytes(DOTTED));
                assertEquals(Arrays.asList("obf.Probe", DOTTED), state.loader.names);
                assertEquals(Arrays.asList(DOTTED + ":obf.Probe", DOTTED + ":" + DOTTED), calls);
                state.exclusions("obf.");
                assertSame(result, state.compiler.getBytes(NAME));
                state.exclusions(DOTTED);
                assertSame(raw, state.compiler.getBytes(NAME));
                assertEquals(3, calls.size());
            } finally {
                obfuscated.setBoolean(null, oldObfuscated);
                mappings.set(FMLDeobfuscatingRemapper.INSTANCE, previous);
            }
        }
    }

    @Test
    void classNodeCachesNormalizedRawBytesButReturnsFreshNodesWithExpandedFrames() throws Exception {
        try (State state = new State()) {
            byte[] bytes = definition(NAME);
            state.loader.bytes = bytes;
            ClassNode first = state.compiler.classNode(DOTTED);
            assertEquals(NAME, first.name);
            FrameNode frame = null;
            for (AbstractInsnNode insn : first.methods.get(0).instructions.toArray()) {
                if (insn instanceof FrameNode) frame = (FrameNode) insn;
            }
            assertNotNull(frame);
            assertEquals(F_NEW, frame.type);
            assertEquals(Arrays.asList(INTEGER), frame.local);
            assertSame(bytes, state.bytes.apply(NAME));
            assertFalse(state.bytes.contains(DOTTED));
            first.methods.clear();
            // Class version is read again from the retained array, rather than from a cached node or copied array.
            bytes[7] = V1_8;
            ClassNode second = ASMMixinCompiler.classNode(NAME);
            assertNotSame(first, second);
            assertEquals(1, second.methods.size());
            assertEquals(V1_8, second.version);
            assertEquals(Arrays.asList(DOTTED), state.loader.names);
        }
    }

    @Test
    void classNodeCachesNullResultsIncludingObjectAndDoesNotCacheNullNameFailures() throws Exception {
        try (State state = new State()) {
            assertNull(state.compiler.classNode(NAME));
            assertTrue(state.bytes.get(NAME).isDefined());
            assertNull(state.bytes.apply(NAME));
            state.loader.bytes = definition(NAME);
            assertNull(state.compiler.classNode(DOTTED));
            assertEquals(1, state.loader.names.size());
            state.bytes.remove("java/lang/Object");
            assertNull(state.compiler.classNode("java.lang.Object"));
            assertTrue(state.bytes.get("java/lang/Object").isDefined());
            assertEquals(1, state.loader.names.size());
            assertThrows(NullPointerException.class, () -> state.compiler.classNode(null));
            assertFalse(state.bytes.contains(null));
        }
    }

    @Test
    void loadFailuresAreRetriedButParseFailuresKeepTheCachedBytes() throws Exception {
        try (State state = new State()) {
            IOException failure = new IOException("read probe");
            state.loader.failure = failure;
            assertSame(failure, assertThrows(IOException.class, () -> state.compiler.classNode(NAME)));
            assertFalse(state.bytes.contains(NAME));
            state.loader.failure = null;
            byte[] malformed = new byte[0];
            state.loader.bytes = malformed;
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> state.compiler.classNode(NAME));
            assertSame(malformed, state.bytes.apply(NAME));
            state.loader.bytes = definition(NAME);
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> state.compiler.classNode(DOTTED));
            assertEquals(2, state.loader.names.size());
            state.bytes.remove(NAME);
            assertEquals(NAME, state.compiler.classNode(NAME).name);
            assertEquals(3, state.loader.names.size());
        }
    }

    @Test
    void internalDefinePublishesTheSameArrayAndInvalidatesOnlyNormalizedMetadataWithoutDefiningAClass()
            throws Exception {
        try (State state = new State()) {
            state.info.put(NAME, null);
            state.info.put(DOTTED, null);
            byte[] bytes = definition(NAME);
            ASMMixinCompiler.internalDefine(DOTTED, bytes);
            assertSame(bytes, state.bytes.apply(NAME));
            assertFalse(state.info.contains(NAME));
            assertTrue(state.info.contains(DOTTED));
            assertEquals(NAME, state.compiler.classNode(DOTTED).name);
            assertTrue(state.loader.names.isEmpty());
            java.lang.reflect.Method loaded = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            loaded.setAccessible(true);
            assertNull(loaded.invoke(state.loader, DOTTED));
        }
    }

    @Test
    void internalDefineKeepsCacheChangesWhenDumpingFailsAndAcceptsNullsWithDebugDisabled() throws Exception {
        try (State state = new State()) {
            DebugPrinter$ printer = DebugPrinter$.MODULE$;
            Field debug = field(DebugPrinter$.class, "debug");
            boolean oldDebug = debug.getBoolean(printer);
            String name = NAME + "DumpFailure";
            Path blockedDump = printer.dir().toPath().resolve(name.replace('/', '#') + ".txt");
            Files.createDirectory(blockedDump);
            try {
                debug.setBoolean(printer, true);
                state.info.put(name, null);
                byte[] bytes = definition(name);
                RuntimeException thrown = assertThrows(
                        RuntimeException.class,
                        () -> state.compiler.internalDefine(name, bytes));
                assertInstanceOf(IOException.class, thrown.getCause());
                assertSame(bytes, state.bytes.apply(name));
                assertFalse(state.info.contains(name));
                debug.setBoolean(printer, false);
                state.info.put(null, null);
                state.compiler.internalDefine(null, null);
                assertTrue(state.bytes.get(null).isDefined());
                assertNull(state.bytes.apply(null));
                assertFalse(state.info.contains(null));
                assertNull(state.compiler.classNode(null));
                assertTrue(state.loader.names.isEmpty());
            } finally {
                debug.setBoolean(printer, oldDebug);
                Files.delete(blockedDump);
            }
        }
    }

    private static byte[] definition(String name) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V1_7, ACC_PUBLIC, name, null, "java/lang/Object", null);
        MethodVisitor body = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "choose", "(Z)I", null, null);
        body.visitCode();
        body.visitVarInsn(ILOAD, 0);
        Label other = new Label();
        body.visitJumpInsn(IFEQ, other);
        body.visitInsn(ICONST_1);
        body.visitInsn(IRETURN);
        body.visitLabel(other);
        body.visitFrame(F_SAME, 0, null, 0, null);
        body.visitInsn(ICONST_2);
        body.visitInsn(IRETURN);
        body.visitMaxs(1, 1);
        body.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Field writableStaticFinal(Class<?> owner, String name) throws Exception {
        // The Forge harness runs on Java 8. Temporarily switch the real environment flag to exercise remapping.
        Field result = field(owner, name);
        field(Field.class, "modifiers").setInt(result, result.getModifiers() & ~Modifier.FINAL);
        return result;
    }

    private static class RecordingLoader extends LaunchClassLoader {

        final List<String> names = new ArrayList<>();
        byte[] bytes;
        IOException failure;

        RecordingLoader() {
            super(new URL[0]);
        }

        @Override
        public byte[] getClassBytes(String name) throws IOException {
            names.add(name);
            if (failure != null) throw failure;
            return bytes;
        }
    }

    /** Restores the singleton, loader and both cache contents even if a characterization assertion fails. */
    private static class State implements AutoCloseable {

        final ASMMixinCompiler$ compiler = ASMMixinCompiler$.MODULE$;
        final Field loaderField = field(ASMMixinCompiler$.class, "cl");
        final Object oldLoader = loaderField.get(compiler);
        final RecordingLoader loader = new RecordingLoader();
        final Map<String, byte[]> bytes = cache("traitByteMap");
        final Map<String, ClassInfo> info = cache("infoCache");
        final java.util.Map<String, byte[]> oldBytes = new HashMap<>(JavaConversions.mapAsJavaMap(bytes));
        final java.util.Map<String, ClassInfo> oldInfo = new HashMap<>(JavaConversions.mapAsJavaMap(info));

        State() throws Exception {
            assertFalse(ObfMapping.obfuscated);
            // Initialize the printer before substituting the loader.
            assertNotNull(DebugPrinter$.MODULE$.dir());
            exclusions();
            bytes.remove(NAME);
            bytes.remove(null);
            loaderField.set(compiler, loader);
        }

        void exclusions(String... prefixes) throws Exception {
            setExclusions(new LinkedHashSet<>(Arrays.asList(prefixes)));
        }

        void setExclusions(Object prefixes) throws Exception {
            compiler.f_transformerExceptions().set(loader, prefixes);
        }

        void transformers(IClassTransformer... transformers) throws Exception {
            field(LaunchClassLoader.class, "transformers").set(loader, Arrays.asList(transformers));
        }

        @SuppressWarnings("unchecked")
        private <T> Map<String, T> cache(String name) throws Exception {
            return (Map<String, T>) field(ASMMixinCompiler$.class, name).get(compiler);
        }

        @Override
        public void close() throws Exception {
            loaderField.set(compiler, oldLoader);
            bytes.clear();
            oldBytes.forEach(bytes::put);
            info.clear();
            oldInfo.forEach(info::put);
            loader.close();
        }
    }
}
