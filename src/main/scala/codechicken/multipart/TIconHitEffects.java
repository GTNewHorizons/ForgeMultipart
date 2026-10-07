package codechicken.multipart;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Particle callbacks for parts that also extend {@link TMultiPart}.
 * <p>
 * TMultiPart declares both members, and a superclass method beats an interface default on the JVM, so implementors must
 * override them themselves and call the {@link IconHitEffects} statics. Declaring them default here would silently lose
 * to TMultiPart's empty versions.
 */
public interface TIconHitEffects extends JIconHitEffects {

    @SideOnly(Side.CLIENT)
    void addHitEffects(MovingObjectPosition hit, EffectRenderer effectRenderer);

    @SideOnly(Side.CLIENT)
    void addDestroyEffects(EffectRenderer effectRenderer);
}
