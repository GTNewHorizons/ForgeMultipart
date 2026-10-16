package codechicken.microblock.handler;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.Item;

import org.apache.logging.log4j.Logger;

import codechicken.lib.config.ConfigFile;
import codechicken.microblock.ItemMicroPart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class MicroblockProxy {

    private MicroblockProxy() {}

    public static Logger logger() {
        return MicroblockProxy$.MODULE$.logger();
    }

    public static void logger_$eq(Logger logger) {
        MicroblockProxy$.MODULE$.logger_$eq(logger);
    }

    public static ItemMicroPart itemMicro() {
        return MicroblockProxy$.MODULE$.itemMicro();
    }

    public static void itemMicro_$eq(ItemMicroPart itemMicro) {
        MicroblockProxy$.MODULE$.itemMicro_$eq(itemMicro);
    }

    public static Item sawStone() {
        return MicroblockProxy$.MODULE$.sawStone();
    }

    public static void sawStone_$eq(Item sawStone) {
        MicroblockProxy$.MODULE$.sawStone_$eq(sawStone);
    }

    public static Item sawIron() {
        return MicroblockProxy$.MODULE$.sawIron();
    }

    public static void sawIron_$eq(Item sawIron) {
        MicroblockProxy$.MODULE$.sawIron_$eq(sawIron);
    }

    public static Item sawDiamond() {
        return MicroblockProxy$.MODULE$.sawDiamond();
    }

    public static void sawDiamond_$eq(Item sawDiamond) {
        MicroblockProxy$.MODULE$.sawDiamond_$eq(sawDiamond);
    }

    public static Item stoneRod() {
        return MicroblockProxy$.MODULE$.stoneRod();
    }

    public static void stoneRod_$eq(Item stoneRod) {
        MicroblockProxy$.MODULE$.stoneRod_$eq(stoneRod);
    }

    public static boolean useSawIcons() {
        return MicroblockProxy$.MODULE$.useSawIcons();
    }

    public static void useSawIcons_$eq(boolean useSawIcons) {
        MicroblockProxy$.MODULE$.useSawIcons_$eq(useSawIcons);
    }

    public static void preInit(Logger logger) {
        MicroblockProxy$.MODULE$.preInit(logger);
    }

    public static Item createSaw(ConfigFile config, String name, int strength) {
        return MicroblockProxy$.MODULE$.createSaw(config, name, strength);
    }

    public static void addSawRecipe(Item saw, Item blade) {
        MicroblockProxy$.MODULE$.addSawRecipe(saw, blade);
    }

    public static RenderBlocks renderBlocks() {
        return MicroblockProxy$.MODULE$.renderBlocks();
    }

    @SideOnly(Side.CLIENT)
    public static void init() {
        MicroblockProxy$.MODULE$.init();
    }

    @SideOnly(Side.CLIENT)
    public static void postInit() {
        MicroblockProxy$.MODULE$.postInit();
    }
}
