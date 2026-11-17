package codechicken.microblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import codechicken.lib.vec.BlockCoord;

public class ExpandingPlacement extends ExecutablePlacement {

    private final Microblock opart;

    public ExpandingPlacement(BlockCoord pos, Microblock part, Microblock opart) {
        super(pos, part);
        this.opart = opart;
    }

    @Override
    public void place(World world, EntityPlayer player, ItemStack item) {
        opart.shape_$eq(part().shape());
        opart.tile().notifyPartChange(opart);
        opart.sendShapeUpdate();
    }

    @Override
    public void consume(World world, EntityPlayer player, ItemStack item) {
        item.stackSize -= 1;
    }
}
