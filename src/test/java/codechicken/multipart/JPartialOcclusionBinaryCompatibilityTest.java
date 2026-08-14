package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

class JPartialOcclusionBinaryCompatibilityTest {

    @Test
    void loadsScalaImplementorCompiledAgainstReferenceJar() throws Exception {
        byte[] bytecode = loadFixture();
        Class<?> fixtureClass = new FixtureClassLoader(JPartialOcclusion.class.getClassLoader()).define(bytecode);
        Object fixture = fixtureClass.getDeclaredConstructor().newInstance();

        assertTrue(fixture instanceof JPartialOcclusion);
        JPartialOcclusion part = (JPartialOcclusion) fixture;
        assertFalse(part.allowCompleteOcclusion());
        assertFalse(part.getPartialOcclusionBoxes().iterator().hasNext());
    }

    private static byte[] loadFixture() {
        InputStream input = Objects.requireNonNull(
                JPartialOcclusionBinaryCompatibilityTest.class
                        .getResourceAsStream("/compat/ReferenceScalaPartialOcclusion.class.b64"));
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
