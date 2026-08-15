package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

/**
 * Both package objects hold one member, a {@code logger} alias for their proxy's logger.
 * <p>
 * Everything here goes through reflection because {@code package} is a Java keyword:
 * {@code codechicken.multipart.package} cannot be written as a type name in Java source at all. That is not a style
 * choice, it is the reason these two classes have no Java form.
 */
class PackageObjectCharacterizationTest {

    @Test
    void bothPackageObjectsExposeAStaticLoggerAccessor() throws Exception {
        for (String name : new String[] { "codechicken.multipart.package", "codechicken.microblock.package" }) {
            Method logger = Class.forName(name).getDeclaredMethod("logger");

            assertSame(Logger.class, logger.getReturnType(), name);
            assertTrue(Modifier.isPublic(logger.getModifiers()), name);
            assertTrue(Modifier.isStatic(logger.getModifiers()), name);
        }
    }

    @Test
    void bothCarryACompanionSingleton() throws Exception {
        for (String name : new String[] { "codechicken.multipart.package$", "codechicken.microblock.package$" }) {
            Class<?> companion = Class.forName(name);

            assertSame(companion, companion.getField("MODULE$").getType(), name);
        }
    }

    /**
     * The alias reads the proxy's var rather than holding anything, so before preInit has run it reports null instead
     * of failing. The Forge server suite asserts the other half, that after preInit it is the very same instance.
     */
    @Test
    void theAliasReportsWhateverTheProxyHoldsAndIsNullBeforePreInit() throws Exception {
        assertNull(invokeLogger("codechicken.multipart.package"));
        assertNull(invokeLogger("codechicken.microblock.package"));
    }

    private static Object invokeLogger(String name) throws Exception {
        return Class.forName(name).getDeclaredMethod("logger").invoke(null);
    }
}
