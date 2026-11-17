package codechicken.multipart;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import codechicken.lib.vec.Vector3;

/**
 * Binary bridge for Scala implementations compiled against the original trait. Their forwarders call these statics
 * directly, so the bodies cannot delegate back to the instance methods without recursing.
 */
@Deprecated
public abstract class TItemMultiPart$class {

    private TItemMultiPart$class() {}

    public static double getHitDepth(TItemMultiPart part, Vector3 vhit, int side) {
        return JItemMultiPart.hitDepth(vhit, side);
    }

    public static boolean onItemUse(TItemMultiPart part, ItemStack item, EntityPlayer player, World world, int x, int y,
            int z, int side, float hitX, float hitY, float hitZ) {
        return JItemMultiPart.onItemUse(part, item, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    public static void $init$(TItemMultiPart part) {}
}
