package codechicken.multipart;

import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import codechicken.lib.render.EntityDigIconFX;
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

    /**
     * The 0xRRGGBB tint for the hit particle on side, mirroring {@link #getBreakingIcon(Object, int)}. Defaults to the
     * whole-part tint.
     */
    @SideOnly(Side.CLIENT)
    default int getBreakingColour(Object subPart, int side, IBlockAccess world, int x, int y, int z) {
        return getBrokenColour(side, world, x, y, z);
    }

    /**
     * The 0xRRGGBB tint to multiply the particle for side by, {@link EntityDigIconFX#NO_TINT} for none. Override
     * alongside {@link #getBrokenIcon(int)} whenever that icon is greyscale and coloured at render time, or the part
     * breaks into grey particles.
     */
    @SideOnly(Side.CLIENT)
    default int getBrokenColour(int side, IBlockAccess world, int x, int y, int z) {
        return EntityDigIconFX.NO_TINT;
    }
}
