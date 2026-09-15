package codechicken.multipart;

import net.minecraft.client.renderer.RenderBlocks;

import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Interface for parts that are simply a cuboid, having one bounding box.
 * <p>
 * Implementors must extend {@link TMultiPart} and route the three overridden methods to the implementations in
 * {@link JCuboidPart}, which {@link JCuboidPart} itself already does. A superclass method beats an interface default on
 * the JVM, so declaring these as default methods here would silently lose to {@link TMultiPart}'s empty versions.
 */
public interface TCuboidPart {

    /** Return the bounding Cuboid6 for this part. */
    Cuboid6 getBounds();

    Iterable<IndexedCuboid6> getSubParts();

    Iterable<Cuboid6> getCollisionBoxes();

    @SideOnly(Side.CLIENT)
    void drawBreaking(RenderBlocks renderBlocks);
}
