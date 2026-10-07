package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/** Loads a Scala consumer compiled against the reference companion-singleton ABI used by downstream mods. */
class BlockMicroMaterialBinaryCompatibilityTest {

    @Test
    void referenceScalaConsumerStillLinksAgainstBothCompanions() throws Exception {
        Class<?> fixtureClass = new FixtureClassLoader(BlockMicroMaterial.class.getClassLoader()).define(loadFixture());
        Object fixture = fixtureClass.getDeclaredConstructor().newInstance();

        assertEquals(
                "example_3",
                fixtureClass.getMethod("materialKey", String.class, int.class).invoke(fixture, "example", 3));
        assertEquals(0, fixtureClass.getMethod("constructorDefault").invoke(fixture));
        assertEquals(0, fixtureClass.getMethod("registrationDefault").invoke(fixture));
        assertEquals(7, fixtureClass.getMethod("helperPass", int.class).invoke(fixture, 7));
    }

    private static byte[] loadFixture() {
        InputStream input = Objects.requireNonNull(
                BlockMicroMaterialBinaryCompatibilityTest.class
                        .getResourceAsStream("/compat/ReferenceScalaBlockMicroMaterialConsumer.class.b64"));
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
