package codechicken.microblock;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import codechicken.lib.config.ConfigTag;

public class ItemSaw extends Item implements Saw {

    private int harvestLevel;

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

    /** @return the current cutting strength */
    public int harvestLevel() {
        return harvestLevel;
    }

    /**
     * Changes the cutting strength used by recipes and the saw renderer. This does not change the durability selected
     * when the saw was constructed. Startup integrations that change saw tiers must finish before ForgeMicroblock
     * post-initialization, when {@link MicroMaterialRegistry#getMaxCuttingStrength()} is calculated.
     *
     * @param harvestLevel the new cutting strength
     */
    public void setHarvestLevel(int harvestLevel) {
        this.harvestLevel = harvestLevel;
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
