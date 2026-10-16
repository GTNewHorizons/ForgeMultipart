package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;

/**
 * Loads a Scala class compiled against the reference dev jar. Its occlusionTest forwards to TNormalOcclusion$class,
 * which in turn calls back through the generated super accessor, so this covers the whole bridge contract.
 */
class TNormalOcclusionBinaryCompatibilityTest {

    @Test
    void referenceScalaImplementorStillLinksThroughTheBridge() throws Exception {
        Class<?> fixtureClass = new FixtureClassLoader(TNormalOcclusion.class.getClassLoader()).define(loadFixture());
        Object fixture = fixtureClass.getDeclaredConstructor().newInstance();

        assertTrue(fixture instanceof TNormalOcclusion);
        assertTrue(fixture instanceof JNormalOcclusion);
        TNormalOcclusion part = (TNormalOcclusion) fixture;

        // The fixture occupies x 0.0 to 0.5, so a touching neighbour fits and an overlapping one does not.
        assertTrue(part.occlusionTest(new NormallyOccludedPart(new Cuboid6(0.5, 0, 0, 1, 1, 1))));
        assertFalse(part.occlusionTest(new NormallyOccludedPart(new Cuboid6(0.25, 0, 0, 1, 1, 1))));
    }

    private static byte[] loadFixture() {
        InputStream input = Objects.requireNonNull(
                TNormalOcclusionBinaryCompatibilityTest.class
                        .getResourceAsStream("/compat/ReferenceScalaNormalOcclusion.class.b64"));
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
