package codechicken.multipart.minecraft;

import net.minecraftforge.common.MinecraftForge;

import codechicken.lib.packet.PacketCustom;
import codechicken.multipart.Tags;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
        modid = "McMultipart",
        name = "Minecraft Multipart Plugin",
        acceptedMinecraftVersions = "[1.7.10]",
        version = Tags.VERSION)
public class MinecraftMultipartMod {

    @Instance("McMultipart")
    public static MinecraftMultipartMod instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        new Content().init();
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        PacketCustom.assignHandler(this, new McMultipartSPH());
        if (FMLCommonHandler.instance().getSide().isClient()) PacketCustom.assignHandler(this, new McMultipartCPH());
    }
}
