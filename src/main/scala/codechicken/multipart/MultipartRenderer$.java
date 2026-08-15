package codechicken.multipart;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;

import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Vector3;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import scala.Tuple2;

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
        TileMultipartClient client = (TileMultipartClient) t;
        TileMultipart tile = (TileMultipart) t;
        if (tile.jPartList().isEmpty() || !client.hasDynamicParts()) {
            return;
        }

        CCRenderState state = CCRenderState.instance();
        state.resetInstance();
        state.pullLightmapInstance();
        state.useNormals = true;

        client.renderDynamic(new Vector3(x, y, z), f, MinecraftForgeClient.getRenderPass());
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
        if (tile.jPartList().isEmpty()) {
            return false;
        }

        if (renderer.hasOverrideBlockTexture()) {
            drawBreakingPart(tile, x, y, z, renderer);
            return false;
        }

        CCRenderState state = CCRenderState.instance();
        state.resetInstance();
        state.lightMatrix.locate(world, x, y, z);
        boolean b = client.renderStatic(world, new Vector3(x, y, z), renderer);
        state.lightMatrix.access = null;
        return b;
    }

    /**
     * The hit data is a Scala Tuple2 whose first member is the struck part's index. It arrives erased, so both the
     * tuple and the index are checked before use, exactly as the reference's pattern match did.
     */
    private static void drawBreakingPart(TileMultipart tile, int x, int y, int z, RenderBlocks renderer) {
        MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;
        if (hit == null || hit.blockX != x || hit.blockY != y || hit.blockZ != z) {
            return;
        }

        Object data = ExtendedMOP.getData(hit);
        if (!(data instanceof Tuple2)) {
            return;
        }

        Object index = ((Tuple2<?, ?>) data)._1();
        if (!(index instanceof Integer)) {
            return;
        }

        int i = (Integer) index;
        if (i >= 0 && i < tile.jPartList().size()) {
            tile.jPartList().get(i).drawBreaking(renderer);
        }
    }

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks renderer) {}

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }
}
