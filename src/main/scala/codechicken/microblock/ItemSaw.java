package codechicken.microblock;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import codechicken.lib.config.ConfigTag;

public class ItemSaw extends Item implements Saw {

    private final int harvestLevel;

    public ItemSaw(ConfigTag sawTag, int harvestLevel) {
        this.harvestLevel = harvestLevel;
        int maxDamage = sawTag.getTag("durability").getIntValue(1 << (harvestLevel + 8));
        if (maxDamage > 0) {
            setMaxDamage(maxDamage);
        }
        setNoRepair();
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabTools);
    }

    public int harvestLevel() {
        return harvestLevel;
    }

    @Override
    public boolean hasContainerItem() {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        return isDamageable() ? new ItemStack(stack.getItem(), 1, stack.getItemDamage() + 1) : stack;
    }

    @Override
    public boolean doesContainerItemLeaveCraftingGrid(ItemStack stack) {
        return false;
    }

    @Override
    public int getCuttingStrength(ItemStack item) {
        return harvestLevel;
    }
}
