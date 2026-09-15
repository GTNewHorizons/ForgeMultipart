package codechicken.multipart.examples;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;

import codechicken.microblock.BlockMicroMaterial;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.microblock.Microblock;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

/** Forge-tested compiling example for docs/api/MATERIAL_ACCESS.md. */
public final class MaterialAccessExample {

    private MaterialAccessExample() {}

    /** Returns the first usable block ID using GuideNH's metadata suffix policy; failures propagate to the caller. */
    public static String resolvePrimaryMicroblockId(TileEntity tile) {
        if (!(tile instanceof TileMultipart)) return null;
        for (TMultiPart part : ((TileMultipart) tile).jPartList()) {
            if (!(part instanceof Microblock)) continue;
            IMicroMaterial material = MicroMaterialRegistry.getMaterial(((Microblock) part).material());
            if (!(material instanceof BlockMicroMaterial)) continue;
            BlockMicroMaterial blockMaterial = (BlockMicroMaterial) material;
            Block block = blockMaterial.block();
            int meta = blockMaterial.meta();
            if (block == null || block == Blocks.air) continue;
            Object registryName = Block.blockRegistry.getNameForObject(block);
            if (registryName == null) continue;
            String blockId = registryName.toString();
            if (blockId.isEmpty()) continue;
            return meta > 0 ? blockId + ":" + meta : blockId;
        }
        return null;
    }
}
