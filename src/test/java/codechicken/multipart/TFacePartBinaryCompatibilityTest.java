package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/**
 * Loads a Scala class compiled against the reference dev jar, whose forwarders call all three TFacePart$class statics.
 * Recompiling the fixture against the port would hide the linkage failure this exists to catch.
 */
class TFacePartBinaryCompatibilityTest {

    @Test
    void referenceScalaImplementorStillLinksThroughTheBridge() throws Exception {
        Class<?> fixtureClass = new FixtureClassLoader(TFacePart.class.getClassLoader()).define(loadFixture());
        Object fixture = fixtureClass.getDeclaredConstructor().newInstance();

        assertTrue(fixture instanceof TFacePart);
        assertTrue(fixture instanceof TSlottedPart);
        assertTrue(fixture instanceof TMultiPart);
        TFacePart part = (TFacePart) fixture;

        for (int side = 0; side < 6; side++) {
            assertTrue(part.solid(side));
        }
        assertEquals(0, part.redstoneConductionMap());
        assertEquals(0x3f, part.getSlotMask());
    }

    private static byte[] loadFixture() {
        InputStream input = Objects.requireNonNull(
                TFacePartBinaryCompatibilityTest.class.getResourceAsStream("/compat/ReferenceScalaFacePart.class.b64"));
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
