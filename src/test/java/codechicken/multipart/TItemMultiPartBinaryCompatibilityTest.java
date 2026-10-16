package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Vector3;

/**
 * Loads a Scala class compiled against the reference dev jar, whose forwarders call all three TItemMultiPart$class
 * statics. Its newPart returns null, so placement short-circuits before touching the world.
 */
class TItemMultiPartBinaryCompatibilityTest {

    @Test
    void referenceScalaImplementorStillLinksThroughTheBridge() throws Exception {
        Class<?> fixtureClass = new FixtureClassLoader(TItemMultiPart.class.getClassLoader()).define(loadFixture());
        Object fixture = fixtureClass.getDeclaredConstructor().newInstance();

        assertTrue(fixture instanceof TItemMultiPart);
        assertTrue(fixture instanceof Item);
        TItemMultiPart item = (TItemMultiPart) fixture;

        assertEquals(0.75, item.getHitDepth(new Vector3(0.25, 0.5, 0.75), 3));

        Method onItemUse = fixtureClass.getMethod(
                "onItemUse",
                ItemStack.class,
                EntityPlayer.class,
                World.class,
                int.class,
                int.class,
                int.class,
                int.class,
                float.class,
                float.class,
                float.class);
        assertFalse((Boolean) onItemUse.invoke(fixture, null, null, null, 4, 5, 6, 1, 0f, 0.3f, 0f));

        // A shallow hit tries the clicked block first, then its neighbour on that side.
        assertEquals("4,5,6;4,6,6;", fixtureClass.getMethod("attempts").invoke(fixture));
    }

    private static byte[] loadFixture() {
        InputStream input = Objects.requireNonNull(
                TItemMultiPartBinaryCompatibilityTest.class
                        .getResourceAsStream("/compat/ReferenceScalaItemMultiPart.class.b64"));
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
