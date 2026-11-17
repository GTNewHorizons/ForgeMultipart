package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

class DefaultContentCharacterizationTest {

    @Test
    void keepsTheFacadeAndCompanionSurface() throws Exception {
        assertSingletonType(DefaultContent.class, true);
        assertSingletonType(DefaultContent$.class, false);

        assertArrayEquals(new Class<?>[0], DefaultContent.class.getInterfaces());
        assertArrayEquals(new Class<?>[0], DefaultContent$.class.getInterfaces());
        assertEquals(0, DefaultContent.class.getFields().length);

        Field module = DefaultContent$.class.getField("MODULE$");
        assertSame(DefaultContent$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(DefaultContent$.MODULE$, module.get(null));
    }

    private static void assertSingletonType(Class<?> type, boolean staticMethod) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));

        Method[] methods = type.getDeclaredMethods();
        assertEquals(1, methods.length);
        assertEquals("load()V", methods[0].getName() + Type.getMethodDescriptor(methods[0]));
        assertTrue(Modifier.isPublic(methods[0].getModifiers()));
        assertEquals(staticMethod, Modifier.isStatic(methods[0].getModifiers()));
        assertEquals(0, methods[0].getDeclaredAnnotations().length);
    }
}
