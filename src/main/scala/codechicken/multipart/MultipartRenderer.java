package codechicken.multipart;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Static forwarders for {@link MultipartRenderer$}, which holds the implementation. guidenh resolves renderWorldBlock
 * here first and only falls back to the companion's MODULE$, so these must keep their exact signatures.
 */
@SideOnly(Side.CLIENT)
public final class MultipartRenderer {

    private MultipartRenderer() {}

    public static void renderTileEntityAt(TileEntity t, double x, double y, double z, float f) {
        MultipartRenderer$.MODULE$.renderTileEntityAt(t, x, y, z, f);
    }

    public static int getRenderId() {
        return MultipartRenderer$.MODULE$.getRenderId();
    }

    public static boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
            RenderBlocks renderer) {
        return MultipartRenderer$.MODULE$.renderWorldBlock(world, x, y, z, block, modelId, renderer);
    }

    public static void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks renderer) {
        MultipartRenderer$.MODULE$.renderInventoryBlock(block, meta, modelId, renderer);
    }

    public static boolean shouldRender3DInInventory(int modelId) {
        return MultipartRenderer$.MODULE$.shouldRender3DInInventory(modelId);
    }
}
