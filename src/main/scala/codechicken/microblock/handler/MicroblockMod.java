package codechicken.microblock.handler;

import codechicken.microblock.AngelicaCompat;
import codechicken.multipart.Tags;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms.IMCEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;

@Mod(
        modid = "ForgeMicroblock",
        name = "Forge Microblocks",
        acceptedMinecraftVersions = "[1.7.10]",
        dependencies = "required-after:CodeChickenCore@[1.4.3,);required-after:ForgeMultipart",
        version = Tags.VERSION,
        modLanguage = "scala")
public final class MicroblockMod {

    private MicroblockMod() {}

    @Mod.EventHandler
    public static void preInit(FMLPreInitializationEvent event) {
        MicroblockMod$.MODULE$.preInit(event);
    }

    @Mod.EventHandler
    public static void init(FMLInitializationEvent event) {
        MicroblockMod$.MODULE$.init(event);
    }

    @Mod.EventHandler
    public static void postInit(FMLPostInitializationEvent event) {
        MicroblockMod$.MODULE$.postInit(event);
    }

    @Mod.EventHandler
    public static void beforeServerStart(FMLServerAboutToStartEvent event) {
        MicroblockMod$.MODULE$.beforeServerStart(event);
    }

    @Mod.EventHandler
    public static void handleIMC(IMCEvent event) {
        MicroblockMod$.MODULE$.handleIMC(event);
    }

    public static AngelicaCompat angelicaCompat() {
        return MicroblockMod$.MODULE$.angelicaCompat();
    }

    public static void angelicaCompat_$eq(AngelicaCompat angelicaCompat) {
        MicroblockMod$.MODULE$.angelicaCompat_$eq(angelicaCompat);
    }
}
