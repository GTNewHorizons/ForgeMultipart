package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import codechicken.microblock.handler.MicroblockProxy;
import codechicken.multipart.handler.MultipartProxy;

/**
 * Both proxies publish a logger during preInit, and code across both packages logs through them directly now that the
 * package-object aliases are gone. A plain JVM test only ever sees the uninitialised null, so this is the one place the
 * real instances are observable.
 */
class ProxyLoggerFunctionalTest {

    @Test
    void bothProxiesPublishALoggerDuringPreInit() {
        assertNotNull(MultipartProxy.logger(), "MultipartProxy.logger is set in preInit");
        assertNotNull(MicroblockProxy.logger(), "MicroblockProxy.logger is set in preInit");
    }
}
