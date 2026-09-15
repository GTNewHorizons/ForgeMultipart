package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import net.minecraft.launchwrapper.LaunchClassLoader;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import codechicken.multipart.asm.ASMMixinCompiler;
import codechicken.multipart.asm.ASMMixinCompiler$;
import codechicken.multipart.asm.ASMMixinCompiler.ClassInfo;
import codechicken.multipart.asm.DebugPrinter$;
import scala.collection.JavaConversions;
import scala.collection.mutable.Map;

class ClassDefinitionFunctionalTest {

    private static final String NAME = "codechicken/multipart/test/definition/Probe";
    private static final String MARKER = "forgemultipart.test.definition.initialized";

    @Test
    void publishesAndDumpsTheSuppliedKeyButDefinesTheBytecodeNameWithoutInitializingIt() throws Exception {
        try (State state = new State()) {
            String alias = NAME + "Alias", dotted = alias.replace('/', '.');
            state.info.put(alias, null);
            state.info.put(dotted, null);
            state.debug.setBoolean(state.printer, true);
            byte[] bytes = definition(NAME, 7, true);
            Class<?> defined = ASMMixinCompiler.define(dotted, bytes);
            assertEquals(NAME.replace('/', '.'), defined.getName());
            assertSame(state.loader, defined.getClassLoader());
            assertSame(defined, state.loader.loaded(defined.getName()));
            assertSame(bytes, state.bytes.apply(alias));
            assertFalse(state.bytes.contains(dotted));
            assertFalse(state.info.contains(alias));
            assertTrue(state.info.contains(dotted));
            assertEquals(NAME, state.compiler.classNode(alias).name);
            assertEquals(state.oldUsed + bytes.length, state.used());
            assertNull(System.getProperty(MARKER));
            String dump = new String(Files.readAllBytes(state.dump(alias)), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(dump.contains(NAME));
            assertEquals(7, defined.getMethod("value").invoke(null));
            assertEquals("yes", System.getProperty(MARKER));
        }
    }

    @Test
    void duplicateDefinitionsStayReflectionWrappedAfterReplacingCachesAndCountingBytes() throws Exception {
        try (State state = new State()) {
            byte[] first = definition(NAME, 7, false), second = definition(NAME, 9, false);
            Class<?> defined = state.compiler.define(NAME, first);
            state.info.put(NAME, null);
            InvocationTargetException thrown = assertThrows(
                    InvocationTargetException.class,
                    () -> state.compiler.define(NAME, second));
            assertInstanceOf(LinkageError.class, thrown.getCause());
            assertTrue(thrown.getCause().getMessage().contains("duplicate"));
            assertSame(second, state.bytes.apply(NAME));
            assertFalse(state.info.contains(NAME));
            assertEquals(state.oldUsed + first.length + second.length, state.used());
            assertSame(defined, state.loader.loaded(NAME.replace('/', '.')));
            assertEquals(7, defined.getMethod("value").invoke(null));
        }
    }

    @Test
    void malformedBytesRemainPublishedAndCountedWhenTheJvmRejectsThem() throws Exception {
        try (State state = new State()) {
            byte[] malformed = { 1, 2, 3 };
            state.info.put(NAME, null);
            InvocationTargetException thrown = assertThrows(
                    InvocationTargetException.class,
                    () -> state.compiler.define(NAME, malformed));
            assertInstanceOf(ClassFormatError.class, thrown.getCause());
            assertSame(malformed, state.bytes.apply(NAME));
            assertFalse(state.info.contains(NAME));
            assertEquals(state.oldUsed + malformed.length, state.used());
            assertNull(state.loader.loaded(NAME.replace('/', '.')));
        }
    }

    @Test
    void nullBytesArePublishedBeforeAccountingFails() throws Exception {
        try (State state = new State()) {
            state.info.put(NAME, null);
            assertThrows(NullPointerException.class, () -> state.compiler.define(NAME, null));
            assertTrue(state.bytes.get(NAME).isDefined());
            assertNull(state.bytes.apply(NAME));
            assertFalse(state.info.contains(NAME));
            assertEquals(state.oldUsed, state.used());
            assertNull(state.loader.loaded(NAME.replace('/', '.')));
        }
    }

    @Test
    void nullNamesAreAcceptedWithoutDumpsButFailBeforeAccountingWhenDumpsAreEnabled() throws Exception {
        try (State state = new State()) {
            byte[] bytes = definition(NAME, 7, false);
            Class<?> defined = state.compiler.define(null, bytes);
            assertEquals(NAME.replace('/', '.'), defined.getName());
            assertSame(bytes, state.bytes.apply(null));
            int counted = state.used();
            state.debug.setBoolean(state.printer, true);
            byte[] second = definition(NAME + "Other", 9, false);
            state.info.put(null, null);
            assertThrows(NullPointerException.class, () -> state.compiler.define(null, second));
            assertSame(second, state.bytes.apply(null));
            assertFalse(state.info.contains(null));
            assertEquals(counted, state.used());
            assertNull(state.loader.loaded((NAME + "Other").replace('/', '.')));
        }
    }

    @Test
    void failedDumpsKeepPublicationButPreventAccountingAndClassDefinition() throws Exception {
        try (State state = new State()) {
            Path blocked = state.dump(NAME);
            Files.createDirectory(blocked);
            try {
                state.debug.setBoolean(state.printer, true);
                state.info.put(NAME, null);
                byte[] bytes = definition(NAME, 7, false);
                RuntimeException thrown = assertThrows(
                        RuntimeException.class,
                        () -> state.compiler.define(NAME, bytes));
                assertInstanceOf(IOException.class, thrown.getCause());
                assertSame(bytes, state.bytes.apply(NAME));
                assertFalse(state.info.contains(NAME));
                assertEquals(state.oldUsed, state.used());
                assertNull(state.loader.loaded(NAME.replace('/', '.')));
            } finally {
                Files.delete(blocked);
            }
        }
    }

    @Test
    void accountingFailuresEscapeBeforeTheDuplicateErrorHandler() throws Exception {
        try (State state = new State()) {
            LinkageError failure = new LinkageError("duplicate accounting failure");
            state.counter.setInt(state.printer, 15999);
            state.logger.set(
                    state.printer,
                    Proxy.newProxyInstance(
                            Logger.class.getClassLoader(),
                            new Class<?>[] { Logger.class },
                            (proxy, method, args) -> {
                                assertEquals("debug", method.getName());
                                throw failure;
                            }));
            byte[] bytes = definition(NAME, 7, false);
            assertSame(failure, assertThrows(LinkageError.class, () -> state.compiler.define(NAME, bytes)));
            assertSame(bytes, state.bytes.apply(NAME));
            assertEquals(15999, state.used());
            assertNull(state.loader.loaded(NAME.replace('/', '.')));
        }
    }

    @Test
    void inaccessibleReflectionFailsAfterPublicationAndAccounting() throws Exception {
        try (State state = new State()) {
            state.originalDefine.setAccessible(false);
            byte[] bytes = definition(NAME, 7, false);
            assertThrows(IllegalAccessException.class, () -> state.compiler.define(NAME, bytes));
            assertSame(bytes, state.bytes.apply(NAME));
            assertEquals(state.oldUsed + bytes.length, state.used());
            assertNull(state.loader.loaded(NAME.replace('/', '.')));
        }
    }

    @Test
    void aDirectDuplicateLinkageErrorIsTranslatedWithTheOriginalRequestedNameAndCause() throws Exception {
        try (State state = new State()) {
            state.directFailure("prefix duplicate suffix");
            byte[] bytes = definition(NAME, 7, false);
            String dotted = NAME.replace('/', '.');
            IllegalStateException thrown = assertThrows(
                    IllegalStateException.class,
                    () -> state.compiler.define(dotted, bytes));
            assertEquals(
                    "class with name: " + dotted
                            + " already loaded. Do not reference your java mixin classes before registering",
                    thrown.getMessage());
            assertInstanceOf(LinkageError.class, thrown.getCause());
            assertEquals("prefix duplicate suffix", thrown.getCause().getMessage());
            assertSame(bytes, state.bytes.apply(NAME));
            assertEquals(state.oldUsed + bytes.length, state.used());
        }
    }

    @Test
    void directNonmatchingLinkageErrorsEscapeAndNullMessagesKeepTheGuardFailure() throws Exception {
        for (String message : new String[] { "other failure", "Duplicate", null }) {
            try (State state = new State()) {
                state.directFailure(message);
                byte[] bytes = definition(NAME, 7, false);
                if (message == null) {
                    assertThrows(NullPointerException.class, () -> state.compiler.define(NAME, bytes));
                } else {
                    LinkageError thrown = assertThrows(LinkageError.class, () -> state.compiler.define(NAME, bytes));
                    assertEquals(message, thrown.getMessage());
                }
                assertSame(bytes, state.bytes.apply(NAME));
                assertEquals(state.oldUsed + bytes.length, state.used());
            }
        }
    }

    private static byte[] definition(String name, int value, boolean initialize) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V1_6, ACC_PUBLIC, name, null, "java/lang/Object", null);
        if (initialize) {
            MethodVisitor init = writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            init.visitCode();
            init.visitLdcInsn(MARKER);
            init.visitLdcInsn("yes");
            init.visitMethodInsn(
                    INVOKESTATIC,
                    "java/lang/System",
                    "setProperty",
                    "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                    false);
            init.visitInsn(POP);
            init.visitInsn(RETURN);
            init.visitMaxs(2, 0);
            init.visitEnd();
        }
        MethodVisitor method = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "value", "()I", null, null);
        method.visitCode();
        method.visitIntInsn(BIPUSH, value);
        method.visitInsn(IRETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static class DefinitionLoader extends LaunchClassLoader {

        DefinitionLoader() {
            super(new URL[0]);
        }

        Class<?> loaded(String name) {
            return findLoadedClass(name);
        }

        Class<?> install(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }

    private static class State implements AutoCloseable {

        final ASMMixinCompiler$ compiler = ASMMixinCompiler$.MODULE$;
        final DebugPrinter$ printer = DebugPrinter$.MODULE$;
        final Field cl = field(ASMMixinCompiler$.class, "cl");
        final Object oldLoader = cl.get(compiler);
        final DefinitionLoader loader = new DefinitionLoader();
        final Field define = field(ASMMixinCompiler$.class, "m_defineClass");
        final Method originalDefine = compiler.m_defineClass();
        final boolean oldAccessible = originalDefine.isAccessible();
        final Field debug = field(DebugPrinter$.class, "debug");
        final boolean oldDebug = printer.debug();
        final Field counter = field(DebugPrinter$.class, "permGenUsed");
        final int oldUsed = used();
        final Field logger = field(DebugPrinter$.class, "logger");
        final Object oldLogger = printer.logger();
        final Map<String, byte[]> bytes = cache("traitByteMap");
        final Map<String, ClassInfo> info = cache("infoCache");
        final java.util.Map<String, byte[]> oldBytes = new HashMap<>(JavaConversions.mapAsJavaMap(bytes));
        final java.util.Map<String, ClassInfo> oldInfo = new HashMap<>(JavaConversions.mapAsJavaMap(info));
        final String oldMarker = System.getProperty(MARKER);

        State() throws Exception {
            cl.set(compiler, loader);
            debug.setBoolean(printer, false);
            System.clearProperty(MARKER);
        }

        int used() throws Exception {
            return counter.getInt(printer);
        }

        Path dump(String name) {
            return printer.dir().toPath().resolve(name.replace('/', '#') + ".txt");
        }

        void directFailure(String message) throws Exception {
            // Reflection wraps method-body failures but lets class-initialization LinkageErrors escape directly.
            ClassWriter writer = new ClassWriter(0);
            writer.visit(V1_6, ACC_PUBLIC, NAME + "ReflectionFailure", null, "java/lang/Object", null);
            MethodVisitor init = writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            init.visitCode();
            init.visitTypeInsn(NEW, "java/lang/LinkageError");
            init.visitInsn(DUP);
            if (message == null) init.visitInsn(ACONST_NULL);
            else init.visitLdcInsn(message);
            init.visitMethodInsn(INVOKESPECIAL, "java/lang/LinkageError", "<init>", "(Ljava/lang/String;)V", false);
            init.visitInsn(ATHROW);
            init.visitMaxs(3, 0);
            init.visitEnd();
            MethodVisitor method = writer
                    .visitMethod(ACC_PUBLIC | ACC_STATIC, "define", "([BII)Ljava/lang/Class;", null, null);
            method.visitCode();
            method.visitLdcInsn(Type.getType(Object.class));
            method.visitInsn(ARETURN);
            method.visitMaxs(1, 3);
            method.visitEnd();
            writer.visitEnd();
            define.set(
                    compiler,
                    loader.install(writer.toByteArray()).getMethod("define", byte[].class, int.class, int.class));
        }

        @SuppressWarnings("unchecked")
        private <T> Map<String, T> cache(String name) throws Exception {
            return (Map<String, T>) field(ASMMixinCompiler$.class, name).get(compiler);
        }

        @Override
        public void close() throws Exception {
            cl.set(compiler, oldLoader);
            define.set(compiler, originalDefine);
            originalDefine.setAccessible(oldAccessible);
            debug.setBoolean(printer, oldDebug);
            counter.setInt(printer, oldUsed);
            logger.set(printer, oldLogger);
            bytes.clear();
            oldBytes.forEach(bytes::put);
            info.clear();
            oldInfo.forEach(info::put);
            if (oldMarker == null) System.clearProperty(MARKER);
            else System.setProperty(MARKER, oldMarker);
            loader.close();
        }
    }
}
