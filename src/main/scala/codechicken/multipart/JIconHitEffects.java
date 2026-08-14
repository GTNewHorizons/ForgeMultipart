package codechicken.multipart;

import net.minecraft.util.IIcon;

import codechicken.lib.vec.Cuboid6;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Java interface containing callbacks for particle rendering. Make sure to override addHitEffects and addDestroyEffects
 * on {@link TMultiPart} as {@link TIconHitEffects} does, calling the {@link IconHitEffects} statics.
 */
public interface JIconHitEffects {

    Cuboid6 getBounds();

    @SideOnly(Side.CLIENT)
    default IIcon getBreakingIcon(Object subPart, int side) {
        return getBrokenIcon(side);
    }

    @SideOnly(Side.CLIENT)
    IIcon getBrokenIcon(int side);
}
