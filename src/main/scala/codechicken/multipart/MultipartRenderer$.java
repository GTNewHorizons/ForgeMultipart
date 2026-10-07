package codechicken.multipart;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Vector3;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Internal class for rendering callbacks. Should be moved to the handler package.
 * <p>
 * This is the Scala companion singleton, and it is not a mere forwarder: Forge registers this instance itself, through
 * both {@code RenderingRegistry.registerBlockHandler} and {@code ClientRegistry.bindTileEntitySpecialRenderer}, so the
 * implementation has to live here. guidenh also reads {@code MODULE$} as its fallback path.
 * <p>
 * Claiming the render id in the constructor preserves the reference's timing: a Scala object runs its body on first
 * access, and this class is first touched from the client proxy's postInit.
 */
@SideOnly(Side.CLIENT)
public final class MultipartRenderer$ extends TileEntitySpecialRenderer implements ISimpleBlockRenderingHandler {

    public static final MultipartRenderer$ MODULE$ = new MultipartRenderer$();

    private MultipartRenderer$() {
        TileMultipart.renderID_$eq(RenderingRegistry.getNextAvailableRenderId());
    }

    @Override
    public void renderTileEntityAt(TileEntity t, double x, double y, double z, float f) {
        TileMultipart tile = TileMultipart.class.cast((TileMultipartClient) t);
        if (tile.partList().isEmpty() || !tile.hasDynamicParts()) {
            return;
        }

        CCRenderState state = CCRenderState.instance();
        state.resetInstance();
        state.pullLightmapInstance();
        state.useNormals = true;

        tile.renderDynamic(new Vector3(x, y, z), f, MinecraftForgeClient.getRenderPass());
    }

    @Override
    public int getRenderId() {
        return TileMultipart.renderID();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
            RenderBlocks renderer) {
        TileEntity t = world.getTileEntity(x, y, z);
        if (!(t instanceof TileMultipartClient)) {
            return false;
        }

        TileMultipartClient client = (TileMultipartClient) t;
        TileMultipart tile = (TileMultipart) t;
        if (tile.partList().isEmpty()) {
            return false;
        }

        if (renderer.hasOverrideBlockTexture()) {
            drawBreakingPart(world, x, y, z, renderer);
            return false;
        }

        CCRenderState state = CCRenderState.instance();
        state.resetInstance();
        state.lightMatrix.locate(world, x, y, z);
        boolean b = tile.renderStatic(world, new Vector3(x, y, z), renderer);
        state.lightMatrix.access = null;
        return b;
    }

    private static void drawBreakingPart(IBlockAccess world, int x, int y, int z, RenderBlocks renderer) {
        TMultiPart part = RenderPartResolver.resolve(world, x, y, z);
        if (part instanceof ISBRHPart) {
            ((ISBRHPart) part).renderWorldBlock(world, x, y, z, renderer);
        } else if (part != null) {
            part.drawBreaking(renderer);
        }
    }

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks renderer) {}

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }
}
