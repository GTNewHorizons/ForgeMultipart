package codechicken.multipart;

import java.util.Collections;

import net.minecraft.client.renderer.RenderBlocks;

import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.render.BlockRenderer;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.uv.IconTransformation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Translation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Java class implementation of {@link TCuboidPart}, and the canonical home of its behavior. */
public abstract class JCuboidPart extends TMultiPart implements TCuboidPart {

    @Override
    public Iterable<IndexedCuboid6> getSubParts() {
        return subParts(this);
    }

    @Override
    public Iterable<Cuboid6> getCollisionBoxes() {
        return collisionBoxes(this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void drawBreaking(RenderBlocks renderBlocks) {
        renderBreaking(this, renderBlocks);
    }

    public static Iterable<IndexedCuboid6> subParts(TCuboidPart part) {
        return Collections.singletonList(new IndexedCuboid6(0, part.getBounds()));
    }

    public static Iterable<Cuboid6> collisionBoxes(TCuboidPart part) {
        return Collections.singletonList(part.getBounds());
    }

    @SideOnly(Side.CLIENT)
    public static void renderBreaking(TCuboidPart part, RenderBlocks renderBlocks) {
        TMultiPart multiPart = (TMultiPart) part;
        CCRenderState state = CCRenderState.instance();
        state.resetInstance();
        state.setPipelineInstance(
                new Translation(multiPart.x(), multiPart.y(), multiPart.z()),
                new IconTransformation(renderBlocks.overrideBlockTexture));
        BlockRenderer.renderCuboid(part.getBounds(), 0);
    }
}
