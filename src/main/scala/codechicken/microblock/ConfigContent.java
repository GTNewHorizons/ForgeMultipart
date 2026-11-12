package codechicken.microblock;

import java.io.File;

import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import scala.collection.Seq;

public final class ConfigContent {

    private ConfigContent() {}

    public static void parse(File cfgDir) {
        ConfigContent$.MODULE$.parse(cfgDir);
    }

    public static void generateDefault(File cfgFile) {
        ConfigContent$.MODULE$.generateDefault(cfgFile);
    }

    public static void loadLine(String line) {
        ConfigContent$.MODULE$.loadLine(line);
    }

    public static void loadLines(File cfgFile) {
        ConfigContent$.MODULE$.loadLines(cfgFile);
    }

    public static void load() {
        ConfigContent$.MODULE$.load();
    }

    public static void handleIMC(Seq<IMCMessage> messages) {
        ConfigContent$.MODULE$.handleIMC(messages);
    }
}
