package codechicken.microblock;

import static org.lwjgl.opengl.GL11.*;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

import codechicken.lib.render.BlockRenderer;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.uv.IconTransformation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.multipart.TMultiPart;
import scala.Function5;
import scala.MatchError;
import scala.runtime.AbstractFunction5;
import scala.runtime.BoxedUnit;
import scala.runtime.BoxesRunTime;

/** Client rendering behind the retained Scala inheritance metadata, initializer and super bridge. */
final class HollowMicroblockClientLogic {

    private HollowMicroblockClientLogic() {}

    static void updateRenderMask(HollowMicroblockClient part) {
        part.renderMask_$eq(part.renderMask() & 0xff | part.getHollowSize() << 8);
    }

    static void drawBreaking(HollowMicroblockClient part, RenderBlocks renderBlocks) {
        CCRenderState state = CCRenderState.instance();
        state.resetInstance();
        TMultiPart base = (TMultiPart) part;
        state.setPipelineInstance(
                new Translation(base.x(), base.y(), base.z()),
                new IconTransformation(renderBlocks.overrideBlockTexture));
        part.renderHollow(null, 0, part.getBounds(), 0, false, new BreakingCuboid());
    }

    static void render(HollowMicroblockClient part, Vector3 pos, int pass) {
        if (pass == -1) {
            part.renderHollow(pos, pass, part.getBounds(), 0, false, new RenderCuboid());
        } else if (((Microblock) part).isTransparent()) {
            part.renderHollow(pos, pass, part.renderBounds(), part.renderMask(), false, new RenderCuboid());
        } else {
            part.renderHollow(
                    pos,
                    pass,
                    part.renderBounds(),
                    part.renderMask() | 1 << part.getSlot(),
                    false,
                    new RenderCuboid());
            part.renderHollow(pos, pass, Cuboid6.full, ~(1 << part.getSlot()), true, new RenderCuboid());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void renderHollow(HollowMicroblockClient part, Vector3 pos, int pass, Cuboid6 c, int sideMask, boolean face,
            Function5<?, ?, ?, ?, ?, ?> callback) {
        Function5 f = callback;
        IMicroMaterial material = ((Microblock) part).getIMaterial();
        int size = part.renderMask() >> 8;
        double d1 = 0.5 - size / 32D;
        double d2 = 0.5 + size / 32D;
        double x1 = c.min.x;
        double x2 = c.max.x;
        double y1 = c.min.y;
        double y2 = c.max.y;
        double z1 = c.min.z;
        double z2 = c.max.z;
        int slot = part.getSlot();
        int internalMask = 0;
        switch (slot) {
            case 0:
            case 1:
                if (face) internalMask = 0x3c;
                f.apply(pos, material, pass, new Cuboid6(d1, y1, d2, d2, y2, z2), 0x3b | internalMask);
                f.apply(pos, material, pass, new Cuboid6(d1, y1, z1, d2, y2, d1), 0x37 | internalMask);
                f.apply(pos, material, pass, new Cuboid6(d2, y1, d1, x2, y2, d2), sideMask & 0x23 | 0xc | internalMask);
                f.apply(pos, material, pass, new Cuboid6(x1, y1, d1, d1, y2, d2), sideMask & 0x13 | 0xc | internalMask);
                f.apply(pos, material, pass, new Cuboid6(x1, y1, d2, x2, y2, z2), sideMask & 0x3b | 4 | internalMask);
                f.apply(pos, material, pass, new Cuboid6(x1, y1, z1, x2, y2, d1), sideMask & 0x37 | 8 | internalMask);
                break;
            case 2:
            case 3:
                if (face) internalMask = 0x33;
                f.apply(pos, material, pass, new Cuboid6(d2, d1, z1, x2, d2, z2), 0x2f | internalMask);
                f.apply(pos, material, pass, new Cuboid6(x1, d1, z1, d1, d2, z2), 0x1f | internalMask);
                f.apply(pos, material, pass, new Cuboid6(d1, d2, z1, d2, y2, z2), sideMask & 0xe | 0x30 | internalMask);
                f.apply(pos, material, pass, new Cuboid6(d1, y1, z1, d2, d1, z2), sideMask & 0xd | 0x30 | internalMask);
                f.apply(
                        pos,
                        material,
                        pass,
                        new Cuboid6(d2, y1, z1, x2, y2, z2),
                        sideMask & 0x2f | 0x10 | internalMask);
                f.apply(
                        pos,
                        material,
                        pass,
                        new Cuboid6(x1, y1, z1, d1, y2, z2),
                        sideMask & 0x1f | 0x20 | internalMask);
                break;
            case 4:
            case 5:
                if (face) internalMask = 0xf;
                f.apply(pos, material, pass, new Cuboid6(x1, d2, d1, x2, y2, d2), 0x3e | internalMask);
                f.apply(pos, material, pass, new Cuboid6(x1, y1, d1, x2, d1, d2), 0x3d | internalMask);
                f.apply(pos, material, pass, new Cuboid6(x1, d1, d2, x2, d2, z2), sideMask & 0x38 | 3 | internalMask);
                f.apply(pos, material, pass, new Cuboid6(x1, d1, z1, x2, d2, d1), sideMask & 0x34 | 3 | internalMask);
                f.apply(pos, material, pass, new Cuboid6(x1, d2, z1, x2, y2, z2), sideMask & 0x3e | 1 | internalMask);
                f.apply(pos, material, pass, new Cuboid6(x1, y1, z1, x2, d1, z2), sideMask & 0x3d | 2 | internalMask);
                break;
            default:
                throw new MatchError(slot);
        }
    }

    static boolean drawHighlight(HollowMicroblockClient part, MovingObjectPosition hit, EntityPlayer player,
            float frame) {
        int size = part.getHollowSize();
        double d1 = 0.5 - size / 32D;
        double d2 = 0.5 + size / 32D;
        double thickness = (((Microblock) part).shape() >> 4) / 8D;
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_TEXTURE_2D);
        glColor4f(0, 0, 0, 0.4F);
        glLineWidth(2);
        glDepthMask(false);
        glPushMatrix();
        RenderUtils.translateToWorldCoords(player, frame);
        TMultiPart base = (TMultiPart) part;
        glTranslated(base.x(), base.y(), base.z());
        Rotation.sideRotations[((Microblock) part).shape() & 15].at(Vector3.center).glApply();
        RenderUtils.drawCuboidOutline(new Cuboid6(0, 0, 0, 1, thickness, 1).expand(0.001));
        RenderUtils.drawCuboidOutline(new Cuboid6(d1, 0, d1, d2, thickness, d2).expand(-0.001));
        glPopMatrix();
        glDepthMask(true);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        return true;
    }

    private static final class RenderCuboid
            extends AbstractFunction5<Vector3, IMicroMaterial, Object, Cuboid6, Object, BoxedUnit>
            implements scala.Serializable {

        private static final long serialVersionUID = 0L;

        RenderCuboid() {}

        @Override
        public BoxedUnit apply(Vector3 pos, IMicroMaterial material, Object pass, Cuboid6 box, Object mask) {
            MicroblockRender
                    .renderCuboid(pos, material, BoxesRunTime.unboxToInt(pass), box, BoxesRunTime.unboxToInt(mask));
            return BoxedUnit.UNIT;
        }
    }

    private static final class BreakingCuboid
            extends AbstractFunction5<Vector3, IMicroMaterial, Object, Cuboid6, Object, BoxedUnit>
            implements scala.Serializable {

        private static final long serialVersionUID = 0L;

        BreakingCuboid() {}

        @Override
        public BoxedUnit apply(Vector3 pos, IMicroMaterial material, Object pass, Cuboid6 box, Object mask) {
            BoxesRunTime.unboxToInt(pass);
            BlockRenderer.renderCuboid(box, BoxesRunTime.unboxToInt(mask));
            return BoxedUnit.UNIT;
        }
    }
}
