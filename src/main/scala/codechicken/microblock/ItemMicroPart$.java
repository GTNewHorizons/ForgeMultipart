package codechicken.microblock;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.microblock.handler.MicroblockProxy;

/** Scala-compatible singleton retained for compiled consumers. */
public final class ItemMicroPart$ {

    public static final ItemMicroPart$ MODULE$ = new ItemMicroPart$();

    private ItemMicroPart$() {}

    public void checkTagCompound(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
    }

    public ItemStack create(int damage, int material) {
        return create(damage, MicroMaterialRegistry.materialName(material));
    }

    public ItemStack create(int damage, String material) {
        return create(1, damage, material);
    }

    public ItemStack create(int amount, int damage, String material) {
        ItemStack stack = new ItemStack(MicroblockProxy.itemMicro(), amount, damage);
        checkTagCompound(stack);
        stack.getTagCompound().setString("mat", material);
        return stack;
    }

    public IMicroMaterial getMaterial(ItemStack stack) {
        checkTagCompound(stack);
        if (!stack.getTagCompound().hasKey("mat")) {
            return null;
        }
        IMicroMaterial material = MicroMaterialRegistry.getMaterial(stack.getTagCompound().getString("mat"));
        return material == null ? MissingMicroMaterial$.MODULE$ : material;
    }

    public int getMaterialID(ItemStack stack) {
        checkTagCompound(stack);
        if (!stack.getTagCompound().hasKey("mat")) {
            return MicroMaterialRegistry.getMissingId();
        }
        return MicroMaterialRegistry.materialID(stack.getTagCompound().getString("mat"));
    }
}
