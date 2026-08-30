package codechicken.multipart.handler;

import codechicken.multipart.Tags;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;

@Mod(
        modid = "ForgeMultipart",
        name = "Forge Multipart",
        acceptedMinecraftVersions = "[1.7.10]",
        version = Tags.VERSION,
        modLanguage = "scala")
public final class MultipartMod {

    private MultipartMod() {}

    @Mod.EventHandler
    public static void preInit(FMLPreInitializationEvent event) {
        MultipartMod$.MODULE$.preInit(event);
    }

    @Mod.EventHandler
    public static void init(FMLInitializationEvent event) {
        MultipartMod$.MODULE$.init(event);
    }

    @Mod.EventHandler
    public static void postInit(FMLPostInitializationEvent event) {
        MultipartMod$.MODULE$.postInit(event);
    }

    @Mod.EventHandler
    public static void beforeServerStart(FMLServerAboutToStartEvent event) {
        MultipartMod$.MODULE$.beforeServerStart(event);
    }

    @Mod.EventHandler
    public static void serverStopped(FMLServerStoppedEvent event) {
        MultipartMod$.MODULE$.serverStopped(event);
    }
}
