package codechicken.multipart;

import net.minecraft.client.renderer.RenderBlocks;

import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Binary bridge for Scala implementations compiled against the original trait. Their forwarders call these statics
 * directly, so the bodies cannot delegate back to the instance methods without recursing.
 */
@Deprecated
public abstract class TCuboidPart$class {

    private TCuboidPart$class() {}

    public static Iterable<IndexedCuboid6> getSubParts(TCuboidPart part) {
        return JCuboidPart.subParts(part);
    }

    public static Iterable<Cuboid6> getCollisionBoxes(TCuboidPart part) {
        return JCuboidPart.collisionBoxes(part);
    }

    @SideOnly(Side.CLIENT)
    public static void drawBreaking(TCuboidPart part, RenderBlocks renderBlocks) {
        JCuboidPart.renderBreaking(part, renderBlocks);
    }

    public static void $init$(TCuboidPart part) {}
}
