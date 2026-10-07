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
import org.objectweb.asm.Type;

/**
 * Reflection only: initialization reads the Forge config. DebugPrinterFunctionalTest covers directory cleanup,
 * enabled/disabled dumping and byte-count logging under Forge.
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
