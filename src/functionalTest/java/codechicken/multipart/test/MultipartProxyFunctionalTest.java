package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import codechicken.multipart.handler.MultipartProxy;
import codechicken.multipart.handler.MultipartProxy$;
import codechicken.multipart.handler.MultipartProxy_clientImpl;
import codechicken.multipart.handler.MultipartProxy_serverImpl;

class MultipartProxyFunctionalTest {

    @Test
    void dedicatedServerStripsClientOverridesAndResolvesTheCompanionToServerMethods() throws Exception {
        assertSame(MultipartProxy$.MODULE$, MultipartProxy$.class.getField("MODULE$").get(null));
        assertEquals(0, MultipartProxy_clientImpl.class.getDeclaredMethods().length);
        assertThrows(NoSuchMethodException.class, () -> MultipartProxy.class.getDeclaredMethod("postInit"));
        assertThrows(
                NoSuchMethodException.class,
                () -> MultipartProxy.class.getDeclaredMethod("onTileClassBuilt", Class.class));

        assertSame(MultipartProxy_serverImpl.class, MultipartProxy$.class.getMethod("postInit").getDeclaringClass());
        assertSame(
                MultipartProxy_serverImpl.class,
                MultipartProxy$.class.getMethod("onTileClassBuilt", Class.class).getDeclaringClass());
        assertSame(MultipartProxy_serverImpl.class, MultipartProxy$.class.getMethod("init").getDeclaringClass());

        assertNotNull(MultipartProxy.block());
        assertNotNull(MultipartProxy.config());
        assertNotNull(MultipartProxy.logger());
        assertSame(MultipartProxy.block(), MultipartProxy$.MODULE$.block());
        assertSame(MultipartProxy.config(), MultipartProxy$.MODULE$.config());
        assertSame(MultipartProxy.logger(), MultipartProxy$.MODULE$.logger());
    }
}
