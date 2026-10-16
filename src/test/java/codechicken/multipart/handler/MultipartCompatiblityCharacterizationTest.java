package codechicken.multipart.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.world.World;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import scala.Function4;
import scala.runtime.AbstractFunction4;

class MultipartCompatiblityCharacterizationTest {

    @Test
    void keepsTheTwoStaticFacadesAndCompanionSingletons() throws Exception {
        assertFacade(
                MultipartCompatiblity.class,
                "canAddPart()Lscala/Function4;",
                "canAddPart_$eq(Lscala/Function4;)V",
                "load()V");
        assertCompanion(
                MultipartCompatiblity$.class,
                "canAddPart()Lscala/Function4;",
                "canAddPart_$eq(Lscala/Function4;)V",
                "load()V");
        assertFacade(MCPCCompatModule.class, "load()V");
        assertCompanion(MCPCCompatModule$.class, "load()V");

        Field callback = MultipartCompatiblity$.class.getDeclaredField("canAddPart");
        assertEquals(Function4.class, callback.getType());
        assertTrue(Modifier.isPrivate(callback.getModifiers()));
    }

    @Test
    void defaultPlacementCallbackAlwaysAllows() {
        assertEquals(Boolean.TRUE, MultipartCompatiblity.canAddPart().apply(null, -1, 0, 1));
    }

    @Test
    void facadeAndCompanionShareTheMutableCallback() {
        Function4<World, Object, Object, Object, Object> original = MultipartCompatiblity.canAddPart();
        Function4<World, Object, Object, Object, Object> replacement = new PlacementCallback();
        try {
            MultipartCompatiblity.canAddPart_$eq(replacement);
            assertSame(replacement, MultipartCompatiblity.canAddPart());
            assertSame(replacement, MultipartCompatiblity$.MODULE$.canAddPart());
            assertEquals(Boolean.TRUE, replacement.apply(null, 4, 5, 6));
            assertEquals(Boolean.FALSE, replacement.apply(null, 4, 5, 7));

            MultipartCompatiblity$.MODULE$.canAddPart_$eq(original);
            assertSame(original, MultipartCompatiblity.canAddPart());
        } finally {
            MultipartCompatiblity.canAddPart_$eq(original);
        }
    }

    private static void assertFacade(Class<?> type, String... expectedMethods) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertEquals(new TreeSet<>(Arrays.asList(expectedMethods)), publicMethodSignatures(type));
        assertEquals(0, type.getFields().length);
    }

    private static void assertCompanion(Class<?> type, String... expectedMethods) throws Exception {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertEquals(new TreeSet<>(Arrays.asList(expectedMethods)), publicMethodSignatures(type));

        Field module = type.getField("MODULE$");
        assertSame(type, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(module.get(null), module.get(null));
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

    private static final class PlacementCallback extends AbstractFunction4<World, Object, Object, Object, Object> {

        @Override
        public Object apply(World world, Object x, Object y, Object z) {
            return ((Integer) x) + ((Integer) y) + ((Integer) z) == 15;
        }
    }
}
