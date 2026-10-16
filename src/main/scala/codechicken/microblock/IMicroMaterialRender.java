package codechicken.microblock;

import net.minecraft.world.World;

import codechicken.lib.vec.Cuboid6;

/**
 * Wrapper class blocks/tiles/parts wanting to use the micro material system to render part of their models.
 * <p>
 * The accessors keep their Scala names because TMultiPart already declares world/x/y/z/getRenderBounds under exactly
 * these names, which is what implements this interface for every part.
 */
public interface IMicroMaterialRender {

    /** May be null for inventory rendering. */
    World world();

    int x();

    int y();

    int z();

    /** Return the bounds of the part for texture mapping side decals like grass. */
    Cuboid6 getRenderBounds();
}
