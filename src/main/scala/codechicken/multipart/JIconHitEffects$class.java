package codechicken.multipart;

import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Binary bridge for Scala implementations compiled against the original trait. */
@Deprecated
public abstract class JIconHitEffects$class {

    private JIconHitEffects$class() {}

    @SideOnly(Side.CLIENT)
    public static IIcon getBreakingIcon(JIconHitEffects part, Object subPart, int side) {
        return part.getBrokenIcon(side);
    }

    public static void $init$(JIconHitEffects part) {}
}
