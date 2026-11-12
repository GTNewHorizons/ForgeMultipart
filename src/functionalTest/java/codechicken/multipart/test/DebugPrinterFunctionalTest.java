package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.lib.asm.ASMHelper;
import codechicken.lib.config.ConfigTag;
import codechicken.multipart.asm.DebugPrinter$;
import codechicken.multipart.handler.MultipartProxy;

class DebugPrinterFunctionalTest {

    @Test
    void createsMissingDirectoriesOnlyWhenEnabled(@TempDir Path root) throws Exception {
        for (boolean enabled : new boolean[] { false, true }) {
            Path directory = root.resolve(Boolean.toString(enabled)).resolve("nested");
            Object printer = printer(directory, enabled);
            assertEquals(enabled, Files.isDirectory(directory));

            dump(printer, "fixture/Example");

            assertEquals(enabled, Files.exists(directory.resolve("fixture#Example.txt")));
            assertEquals(enabled, Files.exists(directory));
        }
    }

    @Test
    void cleansOnlyImmediateChildrenAtStartupAndGatesDumps(@TempDir Path root) throws Exception {
        Path expected = root.resolve("expected.txt");
        ASMHelper.dump(classBytes(), expected.toFile(), false, false);
        for (boolean enabled : new boolean[] { false, true }) {
            Path directory = Files.createDirectory(root.resolve(Boolean.toString(enabled)));
            Path stale = Files.write(directory.resolve("stale.txt"), new byte[] { 1, 2, 3 });
            Path empty = Files.createDirectory(directory.resolve("empty"));
            Path nested = Files.createDirectory(directory.resolve("nested"));
            Path nestedFile = Files.write(nested.resolve("keep.txt"), new byte[] { 4, 5, 6 });

            Object printer = printer(directory, enabled);

            assertEquals(!enabled, Files.exists(stale));
            assertEquals(!enabled, Files.exists(empty));
            assertArrayEquals(new byte[] { 4, 5, 6 }, Files.readAllBytes(nestedFile), "Cleanup must not recurse");
            if (!enabled) assertArrayEquals(new byte[] { 1, 2, 3 }, Files.readAllBytes(stale));

            Path later = Files.write(directory.resolve("created-after-startup.txt"), new byte[] { 7 });
            dump(printer, "fixture/Example");
            dump(printer, "fixture/Second");
            assertArrayEquals(new byte[] { 7 }, Files.readAllBytes(later), "Dumping must not repeat startup cleanup");
            for (String name : new String[] { "fixture#Example.txt", "fixture#Second.txt" }) {
                Path output = directory.resolve(name);
                if (enabled) assertArrayEquals(Files.readAllBytes(expected), Files.readAllBytes(output));
                else assertFalse(Files.exists(output));
            }
        }
    }

    @Test
    void logsOnlyOnCumulativeThresholdCrossingsEvenWhenDumpingIsDisabled(@TempDir Path root) throws Exception {
        for (boolean enabled : new boolean[] { false, true }) {
            Object printer = printer(root.resolve(Boolean.toString(enabled)), enabled);
            Logger logger = (Logger) printer.getClass().getMethod("logger").invoke(printer);
            assertEquals("Multipart ASM", logger.getName());
            Level previousLevel = logger.getLevel();
            boolean previousAdditive = logger.isAdditive();
            RecordingAppender appender = new RecordingAppender();
            appender.start();
            logger.addAppender(appender);
            try {
                logger.setLevel(Level.DEBUG);
                logger.setAdditive(false);
                Method defined = printer.getClass().getMethod("defined", String.class, byte[].class);
                defined.invoke(printer, "first", new byte[15999]);
                assertTrue(appender.messages.isEmpty());
                defined.invoke(printer, "boundary", new byte[1]);
                assertEquals(Arrays.asList(message(16000)), appender.messages);
                defined.invoke(printer, "empty", new byte[0]);
                defined.invoke(printer, "below-next", new byte[15999]);
                assertEquals(Arrays.asList(message(16000)), appender.messages);
                defined.invoke(printer, "next-boundary", new byte[1]);
                defined.invoke(printer, "two-boundaries", new byte[32000]);
                defined.invoke(printer, "below-next", new byte[1]);
                assertEquals(Arrays.asList(message(16000), message(32000), message(64000)), appender.messages);
            } finally {
                logger.removeAppender(appender);
                logger.setLevel(previousLevel);
                logger.setAdditive(previousAdditive);
                appender.stop();
            }
        }
    }

    private static String message(int bytes) {
        return bytes + " bytes of permGen has been used by ASMMixinCompiler";
    }

    private static void dump(Object printer, String name) throws Exception {
        printer.getClass().getMethod("dump", String.class, byte[].class).invoke(printer, name, classBytes());
    }

    private static byte[] classBytes() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "fixture/Example", null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static Object printer(Path directory, boolean enabled) throws Exception {
        // Redirect only the hard-coded path; execute the real constructor and methods in an isolated loader.
        ClassNode node = new ClassNode();
        try (InputStream input = DebugPrinter$.class.getResourceAsStream("DebugPrinter$.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        int paths = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("<init>")) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof LdcInsnNode && "asm/multipart".equals(((LdcInsnNode) instruction).cst)) {
                    ((LdcInsnNode) instruction).cst = directory.toAbsolutePath().toString();
                    paths++;
                }
            }
        }
        assertEquals(1, paths, "Never initialize a test copy with the live dev dump path");
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        Class<?> type = new FixtureClassLoader(DebugPrinter$.class.getClassLoader()).define(writer.toByteArray());
        ConfigTag tag = MultipartProxy.config().getTag("debug_asm");
        String previous = tag.value;
        try {
            // In-memory override only: do not save or rewrite the development config file.
            tag.value = Boolean.toString(enabled);
            Object printer = type.getField("MODULE$").get(null);
            assertEquals(enabled, type.getMethod("debug").invoke(printer));
            assertEquals(directory.toAbsolutePath().toFile(), type.getMethod("dir").invoke(printer));
            return printer;
        } finally {
            tag.value = previous;
        }
    }

    private static final class FixtureClassLoader extends ClassLoader {

        private FixtureClassLoader(ClassLoader parent) {
            super(parent);
        }

        private Class<?> define(byte[] bytecode) {
            return defineClass(null, bytecode, 0, bytecode.length);
        }
    }

    private static final class RecordingAppender extends AbstractAppender {

        private final List<String> messages = new ArrayList<>();

        private RecordingAppender() {
            super("fmp-debug-printer-test", null, null, false);
        }

        @Override
        public void append(LogEvent event) {
            assertEquals(Level.DEBUG, event.getLevel());
            messages.add(event.getMessage().getFormattedMessage());
        }
    }
}
