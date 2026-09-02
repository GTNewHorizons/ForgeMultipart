package codechicken.multipart.asm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Reflection and bytecode only: the companion's initializer reads the Forge config, so loading it in a plain JVM is not
 * possible. Dump content and the byte-count log are covered by the Forge suite.
 */
class DebugPrinterCharacterizationTest {

    private static final Set<String> METHODS = signatures(
            "debug()Z",
            "logger()Lorg/apache/logging/log4j/Logger;",
            "dir()Ljava/io/File;",
            "dump(Ljava/lang/String;[B)V",
            "defined(Ljava/lang/String;[B)V");

    @Test
    void keepsExactFacadeAndCompanionSurface() throws Exception {
        for (Class<?> type : new Class<?>[] { DebugPrinter.class, DebugPrinter$.class }) {
            assertEquals(Modifier.PUBLIC | Modifier.FINAL, type.getModifiers());
            assertSame(Object.class, type.getSuperclass());
            assertEquals(0, type.getInterfaces().length);
            assertEquals(METHODS, publicMethods(type));
        }
        for (Method method : DebugPrinter.class.getDeclaredMethods()) {
            assertTrue(Modifier.isStatic(method.getModifiers()));
        }
        assertEquals(0, DebugPrinter.class.getDeclaredFields().length);
    }

    @Test
    void keepsTheCompanionStateThatSurvivesBetweenDumps() throws Exception {
        Field module = DebugPrinter$.class.getDeclaredField("MODULE$");
        assertEquals(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, module.getModifiers());
        assertSame(DebugPrinter$.class, module.getType());
        assertTrue(Modifier.isPrivate(DebugPrinter$.class.getDeclaredConstructor().getModifiers()));

        assertField("debug", boolean.class, Modifier.PRIVATE | Modifier.FINAL);
        assertField("logger", Logger.class, Modifier.PRIVATE | Modifier.FINAL);
        assertField("dir", File.class, Modifier.PRIVATE | Modifier.FINAL);
        // The running permGen total is the only mutable field.
        assertField("permGenUsed", int.class, Modifier.PRIVATE);
        assertEquals(5, DebugPrinter$.class.getDeclaredFields().length);
    }

    @Test
    void gatesOnTheConfigTagAndClearsTheDumpDirectoryOnce() throws Exception {
        ClassNode node = companion();
        assertEquals(signatures("debug_asm", "Multipart ASM", "asm/multipart"), constants(node, "<init>"));
        assertCall(node, "<init>", "codechicken/multipart/handler/MultipartProxy", "config");
        assertCall(node, "<init>", "java/io/File", "mkdirs");
        // The per-file delete itself is covered by the Forge suite, which compares generated dump names.
        assertCall(node, "<init>", "java/io/File", "listFiles");
        assertCall(node, "<init>", "java/io/File", "exists");
    }

    @Test
    void dumpsThroughASMHelperAndLogsEverySixteenThousandBytes() throws Exception {
        ClassNode node = companion();
        assertEquals(signatures(".txt"), constants(node, "dump"));
        assertCall(node, "dump", "codechicken/lib/asm/ASMHelper", "dump");

        assertEquals(signatures(" bytes of permGen has been used by ASMMixinCompiler"), constants(node, "defined"));
        assertTrue(intOperands(node, "defined").contains(16000));
        assertCall(node, "defined", "org/apache/logging/log4j/Logger", "debug");
    }

    private static ClassNode companion() throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(DebugPrinter$.class.getName()).accept(node, 0);
        return node;
    }

    private static Set<String> constants(ClassNode node, String methodName) {
        Set<String> found = new TreeSet<>();
        for (AbstractInsnNode instruction : instructions(node, methodName)) {
            if (instruction instanceof LdcInsnNode && ((LdcInsnNode) instruction).cst instanceof String) {
                found.add((String) ((LdcInsnNode) instruction).cst);
            }
        }
        return found;
    }

    private static Set<Integer> intOperands(ClassNode node, String methodName) {
        Set<Integer> found = new TreeSet<>();
        for (AbstractInsnNode instruction : instructions(node, methodName)) {
            if (instruction instanceof IntInsnNode) {
                found.add(((IntInsnNode) instruction).operand);
            } else if (instruction instanceof LdcInsnNode && ((LdcInsnNode) instruction).cst instanceof Integer) {
                found.add((Integer) ((LdcInsnNode) instruction).cst);
            }
        }
        return found;
    }

    private static void assertCall(ClassNode node, String methodName, String owner, String name) {
        for (AbstractInsnNode instruction : instructions(node, methodName)) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.equals(owner) && call.name.equals(name)) return;
            }
        }
        throw new AssertionError("Missing call " + owner + '.' + name + " in " + methodName);
    }

    private static AbstractInsnNode[] instructions(ClassNode node, String methodName) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(methodName)) return method.instructions.toArray();
        }
        throw new AssertionError("Missing method " + methodName);
    }

    private static void assertField(String name, Class<?> type, int modifiers) throws Exception {
        Field field = DebugPrinter$.class.getDeclaredField(name);
        assertSame(type, field.getType());
        assertEquals(modifiers, field.getModifiers());
    }

    private static Set<String> publicMethods(Class<?> type) {
        Set<String> result = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                result.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return result;
    }

    private static Set<String> signatures(String... values) {
        return new TreeSet<>(Arrays.asList(values));
    }
}
