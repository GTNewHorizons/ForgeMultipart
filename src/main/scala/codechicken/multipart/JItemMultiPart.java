package codechicken.multipart;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;

/** Java class implementation of {@link TItemMultiPart}, and the canonical home of its behavior. */
public abstract class JItemMultiPart extends Item implements TItemMultiPart {

    @Override
    public double getHitDepth(Vector3 vhit, int side) {
        return hitDepth(vhit, side);
    }

    @Override
    public boolean onItemUse(ItemStack item, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        return onItemUse(this, item, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    public static double hitDepth(Vector3 vhit, int side) {
        return vhit.copy().scalarProject(Rotation.axes[side]) + (side % 2 ^ 1);
    }

    public static boolean onItemUse(TItemMultiPart part, ItemStack item, EntityPlayer player, World world, int x, int y,
            int z, int side, float hitX, float hitY, float hitZ) {
        BlockCoord pos = new BlockCoord(x, y, z);
        Vector3 vhit = new Vector3(hitX, hitY, hitZ);

        if (part.getHitDepth(vhit, side) < 1 && place(part, item, player, world, pos, side, vhit)) {
            return true;
        }

        pos.offset(side);
        return place(part, item, player, world, pos, side, vhit);
    }

    private static boolean place(TItemMultiPart part, ItemStack item, EntityPlayer player, World world, BlockCoord pos,
            int side, Vector3 vhit) {
        TMultiPart newPart = part.newPart(item, player, world, pos, side, vhit);
        if (newPart == null || !TileMultipart.canPlacePart(world, pos, newPart)) {
            return false;
        }

        if (!world.isRemote) {
            TileMultipart.addPart(world, pos, newPart);
        }
        if (!player.capabilities.isCreativeMode) {
            item.stackSize -= 1;
        }
        return true;
    }
}
