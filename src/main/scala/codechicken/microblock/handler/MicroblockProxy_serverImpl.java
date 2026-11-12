package codechicken.microblock.handler;

import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import org.apache.logging.log4j.Logger;

import codechicken.lib.config.ConfigFile;
import codechicken.lib.packet.PacketCustom;
import codechicken.microblock.ItemMicroPart;
import codechicken.microblock.ItemSaw;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.MicroRecipe$;
import codechicken.microblock.compat.PosteaCompat;
import codechicken.multipart.handler.MultipartProxy;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import scala.collection.mutable.MutableList;

public class MicroblockProxy_serverImpl {

    private Logger logger;
    private ItemMicroPart itemMicro;
    private Item sawStone;
    private Item sawIron;
    private Item sawDiamond;
    private Item stoneRod;
    private boolean useSawIcons;
    private MutableList<Item> saws = new MutableList<>();

    public Logger logger() {
        return logger;
    }

    public void logger_$eq(Logger logger) {
        this.logger = logger;
    }

    public ItemMicroPart itemMicro() {
        return itemMicro;
    }

    public void itemMicro_$eq(ItemMicroPart itemMicro) {
        this.itemMicro = itemMicro;
    }

    public Item sawStone() {
        return sawStone;
    }

    public void sawStone_$eq(Item sawStone) {
        this.sawStone = sawStone;
    }

    public Item sawIron() {
        return sawIron;
    }

    public void sawIron_$eq(Item sawIron) {
        this.sawIron = sawIron;
    }

    public Item sawDiamond() {
        return sawDiamond;
    }

    public void sawDiamond_$eq(Item sawDiamond) {
        this.sawDiamond = sawDiamond;
    }

    public Item stoneRod() {
        return stoneRod;
    }

    public void stoneRod_$eq(Item stoneRod) {
        this.stoneRod = stoneRod;
    }

    public boolean useSawIcons() {
        return useSawIcons;
    }

    public void useSawIcons_$eq(boolean useSawIcons) {
        this.useSawIcons = useSawIcons;
    }

    public void preInit(Logger logger) {
        this.logger = logger;
        itemMicro = new ItemMicroPart();
        GameRegistry.registerItem(itemMicro, "microblock");
        sawStone = createSaw(MultipartProxy.config(), "sawStone", 1);
        sawIron = createSaw(MultipartProxy.config(), "sawIron", 2);
        sawDiamond = createSaw(MultipartProxy.config(), "sawDiamond", 3);
        stoneRod = new Item().setUnlocalizedName("microblock:stoneRod").setTextureName("microblock:stoneRod");
        GameRegistry.registerItem(stoneRod, "stoneRod");

        OreDictionary.registerOre("rodStone", stoneRod);
        MinecraftForge.EVENT_BUS.register(MicroblockEventHandler$.MODULE$);

        useSawIcons = MultipartProxy.config().getTag("useSawIcons")
                .setComment("Set to true to use mc style icons for the saw instead of the 3D model")
                .getBooleanValue(false);
    }

    public MutableList<Item> saws() {
        return saws;
    }

    public void saws_$eq(MutableList<Item> saws) {
        this.saws = saws;
    }

    public Item createSaw(ConfigFile config, String name, int strength) {
        Item saw = new ItemSaw(config.getTag(name).useBraces(), strength).setUnlocalizedName("microblock:" + name)
                .setTextureName("microblock:" + name);
        GameRegistry.registerItem(saw, name);
        saws.$plus$eq(saw);
        return saw;
    }

    public void addSawRecipe(Item saw, Item blade) {
        recipes().add(
                new ShapedOreRecipe(
                        new ItemStack(saw),
                        "srr",
                        "sbr",
                        Character.valueOf('s'),
                        "stickWood",
                        Character.valueOf('r'),
                        "rodStone",
                        Character.valueOf('b'),
                        blade));
    }

    public void init() {
        recipes().add(MicroRecipe$.MODULE$);
        if (!Loader.isModLoaded("dreamcraft")) {
            CraftingManager.getInstance()
                    .addRecipe(new ItemStack(stoneRod, 4), "s", "s", Character.valueOf('s'), Blocks.stone);
            addSawRecipe(sawStone, Items.flint);
            addSawRecipe(sawIron, Items.iron_ingot);
            addSawRecipe(sawDiamond, Items.diamond);
        }
    }

    public void postInit() {
        MicroMaterialRegistry.calcMaxCuttingStrength();
        PacketCustom.assignHandshakeHandler(MicroblockSPH.registryChannel(), MicroblockSPH$.MODULE$);
        if (Loader.isModLoaded("postea")) {
            PosteaCompat.registerTransformers();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<IRecipe> recipes() {
        return CraftingManager.getInstance().getRecipeList();
    }
}
