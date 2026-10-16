package codechicken.multipart;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Binary bridge for Scala implementations compiled against the original trait. */
@Deprecated
public abstract class TIconHitEffects$class {

    private TIconHitEffects$class() {}

    @SideOnly(Side.CLIENT)
    public static void addHitEffects(TIconHitEffects part, MovingObjectPosition hit, EffectRenderer effectRenderer) {
        IconHitEffects.addHitEffects(part, hit, effectRenderer);
    }

    @SideOnly(Side.CLIENT)
    public static void addDestroyEffects(TIconHitEffects part, EffectRenderer effectRenderer) {
        IconHitEffects.addDestroyEffects(part, effectRenderer);
    }

    public static void $init$(TIconHitEffects part) {}
}
