package codechicken.multipart.handler;

import java.io.File;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;

import org.apache.logging.log4j.Logger;

import codechicken.lib.config.ConfigFile;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.BlockMultipart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class MultipartProxy {

    private MultipartProxy() {}

    public static BlockMultipart block() {
        return MultipartProxy$.MODULE$.block();
    }

    public static void block_$eq(BlockMultipart block) {
        MultipartProxy$.MODULE$.block_$eq(block);
    }

    public static ConfigFile config() {
        return MultipartProxy$.MODULE$.config();
    }

    public static void config_$eq(ConfigFile config) {
        MultipartProxy$.MODULE$.config_$eq(config);
    }

    public static Logger logger() {
        return MultipartProxy$.MODULE$.logger();
    }

    public static void logger_$eq(Logger logger) {
        MultipartProxy$.MODULE$.logger_$eq(logger);
    }

    public static void preInit(File cfgdir, Logger logger) {
        MultipartProxy$.MODULE$.preInit(cfgdir, logger);
    }

    public static void init() {
        MultipartProxy$.MODULE$.init();
    }

    @SideOnly(Side.CLIENT)
    public static void postInit() {
        MultipartProxy$.MODULE$.postInit();
    }

    @SideOnly(Side.CLIENT)
    public static void onTileClassBuilt(Class<? extends TileEntity> type) {
        MultipartProxy$.MODULE$.onTileClassBuilt(type);
    }

    public static BlockCoord indexInChunk(ChunkCoordIntPair chunk, int index) {
        return MultipartProxy$.MODULE$.indexInChunk(chunk, index);
    }

    public static int indexInChunk(BlockCoord pos) {
        return MultipartProxy$.MODULE$.indexInChunk(pos);
    }
}
