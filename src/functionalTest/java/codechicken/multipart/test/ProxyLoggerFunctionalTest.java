package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import codechicken.microblock.handler.MicroblockProxy;
import codechicken.multipart.handler.MultipartProxy;

/**
 * Both proxies publish a logger during preInit, and code across both packages logs through them. A plain JVM test only
 * ever sees the uninitialised null, so this is the one place the real instances are observable.
 */
class ProxyLoggerFunctionalTest {

    @Test
    void bothProxiesPublishALoggerDuringPreInit() {
        assertNotNull(MultipartProxy.logger(), "MultipartProxy.logger is set in preInit");
        assertNotNull(MicroblockProxy.logger(), "MicroblockProxy.logger is set in preInit");
    }

    /**
     * The package objects are pure aliases: each returns the very instance its proxy holds, adding nothing. That is
     * what makes them safe to remove in favour of naming the proxy directly.
     */
    @Test
    void thePackageObjectAliasesAreTheProxyLoggersThemselves() throws Exception {
        assertSame(MultipartProxy.logger(), invokeLogger("codechicken.multipart.package"));
        assertSame(MicroblockProxy.logger(), invokeLogger("codechicken.microblock.package"));
    }

    /** Reflective because {@code package} is a Java keyword and cannot appear as a type name in Java source. */
    private static Object invokeLogger(String name) throws Exception {
        return Class.forName(name).getDeclaredMethod("logger").invoke(null);
    }
}
