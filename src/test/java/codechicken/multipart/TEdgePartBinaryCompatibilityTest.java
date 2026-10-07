package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/**
 * Loads a Scala class compiled against the reference dev jar, whose forwarder calls TEdgePart$class.conductsRedstone
 * and whose constructor calls TEdgePart$class.$init$. OpenComputers links against the latter.
 */
class TEdgePartBinaryCompatibilityTest {

    @Test
    void referenceScalaImplementorStillLinksThroughTheBridge() throws Exception {
        Class<?> fixtureClass = new FixtureClassLoader(TEdgePart.class.getClassLoader()).define(loadFixture());
        Object fixture = fixtureClass.getDeclaredConstructor().newInstance();

        assertTrue(fixture instanceof TEdgePart);
        assertTrue(fixture instanceof TSlottedPart);
        assertTrue(fixture instanceof TMultiPart);
        TEdgePart part = (TEdgePart) fixture;

        assertFalse(part.conductsRedstone());
        assertEquals(1 << 15, part.getSlotMask());
    }

    private static byte[] loadFixture() {
        InputStream input = Objects.requireNonNull(
                TEdgePartBinaryCompatibilityTest.class.getResourceAsStream("/compat/ReferenceScalaEdgePart.class.b64"));
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
