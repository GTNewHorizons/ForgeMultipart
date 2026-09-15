package codechicken.microblock;

import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import scala.Predef$;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.Seq$;

/** Implementation behind the Scala trait declarations needed by inherited and external mixins. */
final class MicroblockTraitLogic {

    private MicroblockTraitLogic() {}

    static IIcon getBrokenIcon(MicroblockClient part, int side) {
        IMicroMaterial material = ((Microblock) part).getIMaterial();
        return material == null ? Blocks.stone.getIcon(0, 0) : material.getBreakingIcon(side);
    }

    static boolean renderStatic(MicroblockClient part, Vector3 pos, int pass) {
        if (((Microblock) part).getIMaterial().canRenderInPass(pass)) {
            part.render(pos, pass);
            return true;
        }
        return false;
    }

    static Cuboid6 getRenderBounds(MicroblockClient part) {
        return part.getBounds();
    }

    static void render(CommonMicroblockClient part, Vector3 pos, int pass) {
        if (pass < 0) {
            MicroblockRender.renderCuboid(pos, ((Microblock) part).getIMaterial(), pass, part.getBounds(), 0);
        } else {
            MicroblockRender.renderCuboid(
                    pos,
                    ((Microblock) part).getIMaterial(),
                    pass,
                    part.renderBounds(),
                    part.renderMask());
        }
    }

    static int getSlot(CommonMicroblock part) {
        return ((Microblock) part).getShape();
    }

    static int getSlotMask(CommonMicroblock part) {
        return 1 << part.getSlot();
    }

    @SuppressWarnings("unchecked")
    static List<Cuboid6> getPartialOcclusionBoxes(CommonMicroblock part) {
        return JavaConversions.seqAsJavaList(
                (Seq<Cuboid6>) Seq$.MODULE$.apply(Predef$.MODULE$.wrapRefArray(new Cuboid6[] { part.getBounds() })));
    }

    static int itemClassID(CommonMicroblock part) {
        return part.microClass().getClassId();
    }
}
