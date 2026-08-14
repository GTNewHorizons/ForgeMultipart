package codechicken.multipart;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;

import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.lib.render.EntityDigIconFX;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;

/**
 * Static implementations of the functions a part would override on {@link TMultiPart} for standard Minecraft style hit
 * and break particles.
 * <p>
 * Java composition setup. A Java class implements {@link JIconHitEffects}, overrides the functions on
 * {@link TMultiPart}, and calls these statics with 'this' as the first parameter. {@link TIconHitEffects} is the
 * equivalent for Scala implementors.
 */
public final class IconHitEffects {

    private IconHitEffects() {}

    public static void addHitEffects(JIconHitEffects part, MovingObjectPosition hit, EffectRenderer effectRenderer) {
        TileMultipart tile = ((TMultiPart) part).tile();
        EntityDigIconFX.addBlockHitEffects(
                tile.getWorldObj(),
                part.getBounds().copy().add(Vector3.fromTileEntity(tile)),
                hit.sideHit,
                part.getBreakingIcon(ExtendedMOP.getData(hit), hit.sideHit),
                effectRenderer);
    }

    public static void addDestroyEffects(JIconHitEffects part, EffectRenderer effectRenderer) {
        addDestroyEffects(part, effectRenderer, true);
    }

    public static void addDestroyEffects(JIconHitEffects part, EffectRenderer effectRenderer, boolean scaleDensity) {
        IIcon[] icons = new IIcon[6];
        for (int i = 0; i < 6; i++) {
            icons[i] = part.getBrokenIcon(i);
        }
        Cuboid6 bounds = scaleDensity ? part.getBounds().copy() : Cuboid6.full.copy();
        // The tile is read only here, after the icons and bounds, matching the reference evaluation order.
        TileMultipart tile = ((TMultiPart) part).tile();
        EntityDigIconFX.addBlockDestroyEffects(
                tile.getWorldObj(),
                bounds.add(Vector3.fromTileEntity(tile)),
                icons,
                effectRenderer);
    }
}
