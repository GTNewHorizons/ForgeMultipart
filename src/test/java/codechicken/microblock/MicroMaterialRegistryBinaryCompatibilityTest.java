package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;

/**
 * Loads a Scala class compiled against the reference dev jar. It reads MicroMaterialRegistry$.MODULE$ and calls the
 * instance methods ProjRed links against, plus the scala.Tuple2 array getIdMap that extrautilities links against.
 *
 * Registration is global, so this registers its own uniquely named material and only asserts facts about it.
 */
class MicroMaterialRegistryBinaryCompatibilityTest {

    private static final String NAME = "test:binarycompat";

    @BeforeAll
    static void register() {
        // The other test class may already have registered the placeholder; re-registering would hit the error path.
        if (MicroMaterialRegistry.getMaterial(MissingMicroMaterial.key()) == null) {
            MicroMaterialRegistry.registerMaterial(MissingMicroMaterial$.MODULE$, MissingMicroMaterial.key());
        }
        if (MicroMaterialRegistry.getMaterial(NAME) == null) {
            MicroMaterialRegistry.registerMaterial(MissingMicroMaterial$.MODULE$, NAME);
        }
        MicroMaterialRegistry$.MODULE$.setupIDMap();
    }

    @Test
    void referenceScalaConsumerStillLinksAgainstTheSingletonAndIdMap() throws Exception {
        Class<?> fixtureClass = new FixtureClassLoader(MicroMaterialRegistry.class.getClassLoader())
                .define(loadFixture());
        Object fixture = fixtureClass.getDeclaredConstructor().newInstance();

        int id = (Integer) fixtureClass.getMethod("idOf", String.class).invoke(fixture, NAME);
        assertEquals(NAME, fixtureClass.getMethod("nameOf", int.class).invoke(fixture, id));

        Object material = fixtureClass.getMethod("materialAt", int.class).invoke(fixture, id);
        assertSame(MicroMaterialRegistry.getMaterial(id), material);
        assertSame(MicroMaterialRegistry.getMaterial(id), (IMicroMaterial) material);

        // Reads the raw scala.Tuple2 array rather than the accessors.
        assertEquals(NAME + "=true", fixtureClass.getMethod("idMapEntry", int.class).invoke(fixture, id));
    }

    private static byte[] loadFixture() {
        InputStream input = Objects.requireNonNull(
                MicroMaterialRegistryBinaryCompatibilityTest.class
                        .getResourceAsStream("/compat/ReferenceScalaMicroMaterialConsumer.class.b64"));
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
