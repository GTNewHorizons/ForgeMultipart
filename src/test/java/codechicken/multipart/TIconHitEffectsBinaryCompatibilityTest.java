package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import net.minecraft.client.particle.EffectRenderer;

import org.junit.jupiter.api.Test;

/**
 * Loads a Scala class compiled against the reference dev jar. Its forwarders call all five statics across both
 * JIconHitEffects$class and TIconHitEffects$class.
 */
class TIconHitEffectsBinaryCompatibilityTest {

    @Test
    void referenceScalaImplementorStillLinksThroughBothBridges() throws Exception {
        Class<?> fixtureClass = new FixtureClassLoader(TIconHitEffects.class.getClassLoader()).define(loadFixture());
        Object fixture = fixtureClass.getDeclaredConstructor().newInstance();

        assertTrue(fixture instanceof TIconHitEffects);
        assertTrue(fixture instanceof JIconHitEffects);
        assertTrue(fixture instanceof TMultiPart);
        TIconHitEffects part = (TIconHitEffects) fixture;

        // Runs JIconHitEffects$class.getBreakingIcon, which must delegate to the fixture's getBrokenIcon.
        assertNull(part.getBreakingIcon(null, 4));
        assertEquals(4, fixtureClass.getMethod("lastSide").invoke(fixture));

        // Runs TIconHitEffects$class.addDestroyEffects. The fixture's tile is null, so it collects all six icons and
        // then fails on the tile; reaching that point proves the static resolved rather than failing to link.
        InvocationTargetException thrown = assertThrows(
                InvocationTargetException.class,
                () -> fixtureClass.getMethod("addDestroyEffects", EffectRenderer.class).invoke(fixture, (Object) null));
        assertInstanceOf(NullPointerException.class, thrown.getCause());
        assertEquals(5, fixtureClass.getMethod("lastSide").invoke(fixture));
    }

    private static byte[] loadFixture() {
        InputStream input = Objects.requireNonNull(
                TIconHitEffectsBinaryCompatibilityTest.class
                        .getResourceAsStream("/compat/ReferenceScalaIconHitEffects.class.b64"));
        try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
            return Base64.getMimeDecoder().decode(scanner.next());
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
}
