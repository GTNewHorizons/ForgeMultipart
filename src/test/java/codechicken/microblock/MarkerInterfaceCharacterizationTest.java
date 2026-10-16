package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;

/**
 * Both interfaces carry no implementation, so their characterization is their shape. The accessor names matter most:
 * {@code IMicroMaterialRender} is satisfied by {@code TMultiPart}'s Scala-style {@code world/x/y/z}, so renaming any of
 * them to a bean accessor would silently unimplement it for every part.
 */
class MarkerInterfaceCharacterizationTest {

    @Test
    void sidedHollowConnectDeclaresOnlyTheSizeQuery() {
        assertBareInterface(ISidedHollowConnect.class, "getHollowSize(int)int");
    }

    @Test
    void microMaterialRenderKeepsScalaStyleAccessorNames() {
        assertBareInterface(
                IMicroMaterialRender.class,
                "world()net.minecraft.world.World",
                "x()int",
                "y()int",
                "z()int",
                "getRenderBounds()codechicken.lib.vec.Cuboid6");
    }

    @Test
    void markersAreImplementableFromJava() {
        Cuboid6 bounds = new Cuboid6(0, 0, 0, 0.5, 0.5, 0.5);
        MarkedRender render = new MarkedRender(bounds);

        assertTrue(render instanceof ISidedHollowConnect);
        assertTrue(render instanceof IMicroMaterialRender);
        assertEquals(8, render.getHollowSize(2));
        assertNull(render.world());
        assertEquals(1, render.x());
        assertEquals(2, render.y());
        assertEquals(3, render.z());
        assertSame(bounds, render.getRenderBounds());
    }

    private static void assertBareInterface(Class<?> type, String... expectedMembers) {
        assertTrue(type.isInterface(), type.getName() + " must stay an interface");
        assertEquals(
                0,
                type.getInterfaces().length,
                type.getName() + " must carry no superinterface, as the Scala trait emits none");

        Set<String> actual = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            assertTrue(Modifier.isAbstract(method.getModifiers()), method + " must stay abstract");
            assertTrue(Modifier.isPublic(method.getModifiers()), method + " must stay public");
            actual.add(signature(method));
        }
        assertEquals(new TreeSet<>(Arrays.asList(expectedMembers)), actual);
    }

    private static String signature(Method method) {
        StringBuilder out = new StringBuilder(method.getName()).append('(');
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(parameters[i].getName());
        }
        return out.append(')').append(method.getReturnType().getName()).toString();
    }

    private static final class MarkedRender implements ISidedHollowConnect, IMicroMaterialRender {

        private final Cuboid6 bounds;

        MarkedRender(Cuboid6 bounds) {
            this.bounds = bounds;
        }

        @Override
        public int getHollowSize(int side) {
            return 8;
        }

        @Override
        public World world() {
            return null;
        }

        @Override
        public int x() {
            return 1;
        }

        @Override
        public int y() {
            return 2;
        }

        @Override
        public int z() {
            return 3;
        }

        @Override
        public Cuboid6 getRenderBounds() {
            return bounds;
        }
    }
}
