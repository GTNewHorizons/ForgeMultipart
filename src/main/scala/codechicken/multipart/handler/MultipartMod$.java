package codechicken.multipart.handler;

import codechicken.multipart.ControlKeyModifer;
import codechicken.multipart.MultiPartRegistry;
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
public final class MultipartMod$ {

    public static final MultipartMod$ MODULE$ = new MultipartMod$();

    private MultipartMod$() {}

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MultipartProxy$.MODULE$.preInit(event.getModConfigurationDirectory(), event.getModLog());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MultipartProxy$.MODULE$.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (MultiPartRegistry.required()) {
            MultiPartRegistry.postInit();
            MultipartProxy$.MODULE$.postInit();
        }
    }

    @Mod.EventHandler
    public void beforeServerStart(FMLServerAboutToStartEvent event) {
        MultiPartRegistry.beforeServerStart();
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        MultipartSaveLoad.loadingWorld_$eq(null);
        ControlKeyModifer.map().clear();
    }
}
