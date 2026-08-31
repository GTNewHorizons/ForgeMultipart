package codechicken.multipart.handler;

import java.io.File;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.Logger;

import codechicken.lib.config.ConfigFile;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.world.TileChunkLoadHook;
import codechicken.lib.world.WorldExtensionManager;
import codechicken.multipart.BlockMultipart;
import codechicken.multipart.MultipartGenerator;
import codechicken.multipart.TickScheduler$;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.GameRegistry;

public class MultipartProxy_serverImpl {

    private BlockMultipart block;
    private ConfigFile config;
    private Logger logger;

    public BlockMultipart block() {
        return block;
    }

    public void block_$eq(BlockMultipart block) {
        this.block = block;
    }

    public ConfigFile config() {
        return config;
    }

    public void config_$eq(ConfigFile config) {
        this.config = config;
    }

    public Logger logger() {
        return logger;
    }

    public void logger_$eq(Logger logger) {
        this.logger = logger;
    }

    public void preInit(File cfgdir, Logger logger) {
        this.logger = logger;
        config = new ConfigFile(new File(cfgdir, "multipart.cfg")).setComment("Multipart API config file");

        MultipartGenerator
                .registerTrait("codechicken.multipart.TSlottedPart", "codechicken.multipart.scalatraits.TSlottedTile");
        MultipartGenerator.registerTrait(
                "net.minecraftforge.fluids.IFluidHandler",
                "codechicken.multipart.scalatraits.TFluidHandlerTile");
        MultipartGenerator.registerTrait(
                "net.minecraft.inventory.IInventory",
                "codechicken.multipart.scalatraits.JInventoryTile");
        MultipartGenerator.registerTrait(
                "net.minecraft.inventory.ISidedInventory",
                "codechicken.multipart.scalatraits.JInventoryTile");
        MultipartGenerator.registerTrait(
                "codechicken.multipart.JPartialOcclusion",
                "codechicken.multipart.scalatraits.TPartialOcclusionTile");
        MultipartGenerator.registerTrait(
                "codechicken.multipart.IRedstonePart",
                "codechicken.multipart.scalatraits.TRedstoneTile");
        MultipartGenerator.registerTrait(
                "codechicken.multipart.IRandomDisplayTick",
                "codechicken.multipart.scalatraits.TRandomDisplayTickTile",
                null);
        MultipartGenerator.registerTrait(
                "codechicken.multipart.INeighborTileChange",
                null,
                "codechicken.multipart.scalatraits.TTileChangeTile");

        GameRegistry.registerBlock(new BlockMultipart().setBlockName("multipart"), null, "block");
        block = (BlockMultipart) Block.blockRegistry.getObject("ForgeMultipart:block");

        MultipartSaveLoad.hookLoader();
    }

    public void init() {}

    public void postInit() {
        FMLCommonHandler.instance().bus().register(MultipartEventHandler$.MODULE$);
        MinecraftForge.EVENT_BUS.register(MultipartEventHandler$.MODULE$);
        PacketCustom.assignHandler(MultipartSPH$.MODULE$.channel(), MultipartSPH$.MODULE$);
        PacketCustom.assignHandshakeHandler(MultipartSPH$.MODULE$.registryChannel(), MultipartSPH$.MODULE$);

        WorldExtensionManager.registerWorldExtension(TickScheduler$.MODULE$);
        TileChunkLoadHook.init();

        MultipartCompatiblity.load();
    }

    public void onTileClassBuilt(Class<? extends TileEntity> type) {
        MultipartSaveLoad.registerTileClass(type);
    }
}
