package codechicken.microblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.opengl.GL11;

import codechicken.lib.render.BlockRenderer.BlockFace;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.TextureUtils;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;

public final class MicroblockRender$ {

    public static final MicroblockRender$ MODULE$ = new MicroblockRender$();

    private final ThreadLocal<BlockFace> face = ThreadLocal.withInitial(BlockFace::new);

    private MicroblockRender$() {}

    public void renderItem(Microblock part, int size, int slot) {
        MicroblockClient client = (MicroblockClient) part;
        part.setShape(size, slot);
        client.render(new Vector3(0.5, 0.5, 0.5).subtract(client.getBounds().center()), -1);
    }

    public void renderHighlight(EntityPlayer player, MovingObjectPosition hit, CommonMicroClass mcrClass, int size,
            int material) {
        mcrClass.placementProperties().placementGrid().render(new Vector3(hit.hitVec), hit.sideHit);

        ExecutablePlacement placement = MicroblockPlacement$.MODULE$.apply(
                player,
                hit,
                size,
                material,
                !player.capabilities.isCreativeMode,
                mcrClass.placementProperties());
        if (placement == null) {
            return;
        }

        BlockCoord pos = placement.pos();
        MicroblockClient part = (MicroblockClient) placement.part();
        CCRenderState state = CCRenderState.instance();

        GL11.glPushMatrix();
        GL11.glTranslated(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
        GL11.glScaled(1.002, 1.002, 1.002);
        GL11.glTranslated(-0.5, -0.5, -0.5);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(false);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        TextureUtils.bindAtlas(0);
        state.resetInstance();
        state.alphaOverride = 80;
        state.useNormals = true;
        state.startDrawingInstance();
        part.render(Vector3.zero, -1);
        state.drawInstance();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }

    public ThreadLocal<BlockFace> face() {
        return face;
    }

    public void renderCuboid(Vector3 pos, IMicroMaterial material, int pass, Cuboid6 bounds, int faces) {
        BlockFace localFace = face.get();
        CCRenderState.instance().setModelInstance(localFace);
        for (int side = 0; side < 6; side++) {
            if ((faces & 1 << side) == 0) {
                localFace.loadCuboidFace(bounds, side);
                material.renderMicroFace(pos, pass, bounds);
            }
        }
    }
}
