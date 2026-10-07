package codechicken.multipart.test;

import java.lang.reflect.Array;
import java.lang.reflect.Proxy;

import org.apache.logging.log4j.Logger;

import codechicken.multipart.handler.MultipartProxy;

/**
 * Runs an action with ForgeMultipart's logger detached, for tests that deliberately drive a path which logs at ERROR.
 * Those lines are correct behaviour, but in a green run they read as failures.
 *
 * <p>
 * The swap is global and not thread safe; callers run on the server thread inside one synchronous call.
 */
final class ExpectedErrorLog {

    /** A reflective proxy rather than a subclass, so it does not depend on Forge's log4j-api version. */
    private static final Logger SILENT = (Logger) Proxy
            .newProxyInstance(Logger.class.getClassLoader(), new Class<?>[] { Logger.class }, (proxy, method, args) -> {
                Class<?> returnType = method.getReturnType();
                if (returnType == String.class) return "";
                if (returnType == void.class || !returnType.isPrimitive()) return null;
                // Boxed zero/false for any primitive return, without enumerating them.
                return Array.get(Array.newInstance(returnType, 1), 0);
            });

    private ExpectedErrorLog() {}

    static void silence(Runnable action) {
        Logger original = MultipartProxy.logger();
        MultipartProxy.logger_$eq(SILENT);
        try {
            action.run();
        } finally {
            MultipartProxy.logger_$eq(original);
        }
    }
}
