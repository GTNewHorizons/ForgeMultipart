package codechicken.multipart.asm;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import codechicken.lib.asm.ASMHelper;
import codechicken.lib.asm.ObfMapping;
import codechicken.multipart.handler.MultipartProxy;

public final class DebugPrinter$ {

    public static final DebugPrinter$ MODULE$ = new DebugPrinter$();

    private final boolean debug;
    private final Logger logger;
    private int permGenUsed;
    private final File dir;

    private DebugPrinter$() {
        debug = MultipartProxy.config().getTag("debug_asm").getBooleanValue(!ObfMapping.obfuscated);
        logger = LogManager.getLogger("Multipart ASM");
        dir = new File("asm/multipart");
        if (debug) {
            if (!dir.exists()) dir.mkdirs();
            for (File file : dir.listFiles()) {
                file.delete();
            }
        }
    }

    public boolean debug() {
        return debug;
    }

    public Logger logger() {
        return logger;
    }

    public File dir() {
        return dir;
    }

    public void dump(String name, byte[] bytes) {
        if (debug) ASMHelper.dump(bytes, new File(dir, name.replace('/', '#') + ".txt"), false, false);
    }

    public void defined(String name, byte[] bytes) {
        if ((permGenUsed + bytes.length) / 16000 != permGenUsed / 16000) {
            logger.debug((permGenUsed + bytes.length) + " bytes of permGen has been used by ASMMixinCompiler");
        }
        permGenUsed += bytes.length;
    }
}
