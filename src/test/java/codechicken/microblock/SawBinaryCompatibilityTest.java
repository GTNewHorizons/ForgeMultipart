package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import net.minecraft.item.Item;

import org.junit.jupiter.api.Test;

/**
 * Loads a Scala class compiled against the reference dev jar, whose forwarder calls Saw$class.getMaxCuttingStrength and
 * whose constructor calls Saw$class.$init$. ProjRed links against both.
 */
class SawBinaryCompatibilityTest {

    @Test
    void referenceScalaImplementorStillLinksThroughTheBridge() throws Exception {
        Class<?> fixtureClass = new FixtureClassLoader(Saw.class.getClassLoader()).define(loadFixture());
        Object fixture = fixtureClass.getDeclaredConstructor().newInstance();

        assertTrue(fixture instanceof Saw);
        assertTrue(fixture instanceof Item);
        Saw saw = (Saw) fixture;

        // The fixture returns 7 only when handed a stack that actually wraps itself.
        assertEquals(7, saw.getMaxCuttingStrength());
    }

    private static byte[] loadFixture() {
        InputStream input = Objects.requireNonNull(
                SawBinaryCompatibilityTest.class.getResourceAsStream("/compat/ReferenceScalaSaw.class.b64"));
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
