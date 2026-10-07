package codechicken.microblock;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import scala.Tuple3;
import scala.collection.immutable.Map;

/** Static forwarders for Java and existing bytecode; implementation remains on {@link MicroRecipe$}. */
public final class MicroRecipe {

    private MicroRecipe() {}

    public static ItemStack getRecipeOutput() {
        return MicroRecipe$.MODULE$.getRecipeOutput();
    }

    public static int getRecipeSize() {
        return MicroRecipe$.MODULE$.getRecipeSize();
    }

    public static boolean matches(InventoryCrafting crafting, World world) {
        return MicroRecipe$.MODULE$.matches(crafting, world);
    }

    public static ItemStack getCraftingResult(InventoryCrafting crafting) {
        return MicroRecipe$.MODULE$.getCraftingResult(crafting);
    }

    public static ItemStack create(int amount, int microClass, int size, int material) {
        return MicroRecipe$.MODULE$.create(amount, microClass, size, material);
    }

    public static int microMaterial(ItemStack item) {
        return MicroRecipe$.MODULE$.microMaterial(item);
    }

    public static int microClass(ItemStack item) {
        return MicroRecipe$.MODULE$.microClass(item);
    }

    public static int microSize(ItemStack item) {
        return MicroRecipe$.MODULE$.microSize(item);
    }

    public static ItemStack getHollowResult(InventoryCrafting crafting) {
        return MicroRecipe$.MODULE$.getHollowResult(crafting);
    }

    public static ItemStack getGluingResult(InventoryCrafting crafting) {
        return MicroRecipe$.MODULE$.getGluingResult(crafting);
    }

    public static Tuple3<Saw, Object, Object> getSaw(InventoryCrafting crafting) {
        return MicroRecipe$.MODULE$.getSaw(crafting);
    }

    public static boolean canCut(Saw saw, ItemStack sawItem, int material) {
        return MicroRecipe$.MODULE$.canCut(saw, sawItem, material);
    }

    public static ItemStack getThinningResult(InventoryCrafting crafting) {
        return MicroRecipe$.MODULE$.getThinningResult(crafting);
    }

    public static int findMaterial(ItemStack item) {
        return MicroRecipe$.MODULE$.findMaterial(item);
    }

    public static Map<Object, Object> splitMap() {
        return MicroRecipe$.MODULE$.splitMap();
    }

    public static ItemStack getSplittingResult(InventoryCrafting crafting) {
        return MicroRecipe$.MODULE$.getSplittingResult(crafting);
    }

    public static ItemStack getHollowFillResult(InventoryCrafting crafting) {
        return MicroRecipe$.MODULE$.getHollowFillResult(crafting);
    }
}
