package codechicken.multipart.asm;

import java.io.File;

import org.apache.logging.log4j.Logger;

public final class DebugPrinter {

    private DebugPrinter() {}

    public static boolean debug() {
        return DebugPrinter$.MODULE$.debug();
    }

    public static Logger logger() {
        return DebugPrinter$.MODULE$.logger();
    }

    public static File dir() {
        return DebugPrinter$.MODULE$.dir();
    }

    public static void dump(String name, byte[] bytes) {
        DebugPrinter$.MODULE$.dump(name, bytes);
    }

    public static void defined(String name, byte[] bytes) {
        DebugPrinter$.MODULE$.defined(name, bytes);
    }
}
