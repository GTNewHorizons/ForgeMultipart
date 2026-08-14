package codechicken.multipart;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;

/**
 * Simple multipart item interface for easy placement. Override newPart and the part will be added to the block space if
 * it passes the occlusion tests.
 * <p>
 * Implementors must extend {@link net.minecraft.item.Item} and override onItemUse themselves, delegating to
 * {@link JItemMultiPart#onItemUse}, as {@link JItemMultiPart} does. Item declares onItemUse and a superclass method
 * beats an interface default on the JVM, so declaring it default here would silently lose.
 */
public interface TItemMultiPart {

    default double getHitDepth(Vector3 vhit, int side) {
        return JItemMultiPart.hitDepth(vhit, side);
    }

    boolean onItemUse(ItemStack item, EntityPlayer player, World world, int x, int y, int z, int side, float hitX,
            float hitY, float hitZ);

    /** Create a new part based on the placement information parameters. */
    TMultiPart newPart(ItemStack item, EntityPlayer player, World world, BlockCoord pos, int side, Vector3 vhit);
}
