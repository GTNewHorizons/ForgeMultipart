package codechicken.microblock.compat;

import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.postea.api.IItemStackTransformationHandler;

import codechicken.microblock.MicroMaterialRegistry;

public class ItemMicroPartTransformer implements IItemStackTransformationHandler {

    @Override
    public boolean apply(String originalItemId, NBTTagCompound stackTag) {
        if (!stackTag.hasKey("tag")) return false;
        NBTTagCompound tag = stackTag.getCompoundTag("tag");
        if (!tag.hasKey("mat")) return false;

        String matName = tag.getString("mat");

        // Goes through the remap layer, which is private
        int remappedId = MicroMaterialRegistry.materialID(matName);
        String remappedName = MicroMaterialRegistry.materialName(remappedId);

        if (remappedName.equals(matName)) return false;

        tag.setString("mat", remappedName);
        stackTag.setTag("tag", tag);
        return true;
    }
}
