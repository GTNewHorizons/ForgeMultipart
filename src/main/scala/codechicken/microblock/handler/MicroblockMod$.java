package codechicken.microblock.handler;

import codechicken.microblock.AngelicaCompat;
import codechicken.microblock.ConfigContent$;
import codechicken.microblock.DefaultContent$;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.multipart.Tags;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms.IMCEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import scala.collection.JavaConversions;

@Mod(
        modid = "ForgeMicroblock",
        name = "Forge Microblocks",
        acceptedMinecraftVersions = "[1.7.10]",
        dependencies = "required-after:CodeChickenCore@[1.4.3,);required-after:ForgeMultipart",
        version = Tags.VERSION,
        modLanguage = "scala")
public final class MicroblockMod$ {

    public static final MicroblockMod$ MODULE$ = new MicroblockMod$();

    private AngelicaCompat angelicaCompat;

    private MicroblockMod$() {}

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MicroblockProxy$.MODULE$.preInit(event.getModLog());
        DefaultContent$.MODULE$.load();
        ConfigContent$.MODULE$.parse(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MicroblockProxy$.MODULE$.init();
        ConfigContent$.MODULE$.load();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        MicroMaterialRegistry.setupIDMap();
        MicroblockProxy$.MODULE$.postInit();
    }

    @Mod.EventHandler
    public void beforeServerStart(FMLServerAboutToStartEvent event) {
        MicroMaterialRegistry.setupIDMap();
    }

    @Mod.EventHandler
    public void handleIMC(IMCEvent event) {
        ConfigContent$.MODULE$.handleIMC(JavaConversions.asScalaBuffer(event.getMessages()));
    }

    public AngelicaCompat angelicaCompat() {
        return angelicaCompat;
    }

    public void angelicaCompat_$eq(AngelicaCompat angelicaCompat) {
        this.angelicaCompat = angelicaCompat;
    }
}
