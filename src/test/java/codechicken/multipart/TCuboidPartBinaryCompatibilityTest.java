package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;

/**
 * Loads a Scala class compiled against the reference dev jar, whose forwarders call TCuboidPart$class directly.
 * Recompiling the fixture against the port would hide the linkage failure this exists to catch.
 */
class TCuboidPartBinaryCompatibilityTest {

    @Test
    void referenceScalaImplementorStillLinksThroughTheBridge() throws Exception {
        byte[] bytecode = loadFixture();
        Class<?> fixtureClass = new FixtureClassLoader(TCuboidPart.class.getClassLoader()).define(bytecode);
        Object fixture = fixtureClass.getDeclaredConstructor().newInstance();

        assertTrue(fixture instanceof TCuboidPart);
        assertTrue(fixture instanceof TMultiPart);
        TCuboidPart part = (TCuboidPart) fixture;

        Cuboid6 bounds = part.getBounds();
        assertEquals(0.5, bounds.max.x);

        Iterator<IndexedCuboid6> subParts = part.getSubParts().iterator();
        IndexedCuboid6 only = subParts.next();
        assertEquals(Integer.valueOf(0), only.data);
        assertEquals(0.5, only.max.x);
        assertFalse(subParts.hasNext());

        Iterator<Cuboid6> boxes = part.getCollisionBoxes().iterator();
        assertEquals(0.5, boxes.next().max.x);
        assertFalse(boxes.hasNext());
    }

    private static byte[] loadFixture() {
        InputStream input = Objects.requireNonNull(
                TCuboidPartBinaryCompatibilityTest.class
                        .getResourceAsStream("/compat/ReferenceScalaCuboidPart.class.b64"));
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
