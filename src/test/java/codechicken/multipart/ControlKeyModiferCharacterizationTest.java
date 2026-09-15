package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.jupiter.api.Test;

/**
 * Every branch of isControlDown needs a player with a world, so the behavior is covered by the Forge server suite
 * against a FakePlayer. What is pinned here is the Java-facing entry point, which the reference scaladoc calls out as
 * existing for Java callers rather than for the Scala implicit.
 */
class ControlKeyModiferCharacterizationTest {

    /** The class name carries a typo in the reference. It is the published name, so the port keeps it. */
    @Test
    void theTypoInTheClassNameIsPreserved() {
        assertEquals("codechicken.multipart.ControlKeyModifer", ControlKeyModifer.class.getName());
    }

    @Test
    void theJavaFacingQueryIsAPublicStaticTakingAPlayer() throws Exception {
        Method isControlDown = ControlKeyModifer.class.getDeclaredMethod("isControlDown", EntityPlayer.class);

        assertSame(boolean.class, isControlDown.getReturnType());
        assertTrue(Modifier.isPublic(isControlDown.getModifiers()));
        assertTrue(Modifier.isStatic(isControlDown.getModifiers()));
    }
}
