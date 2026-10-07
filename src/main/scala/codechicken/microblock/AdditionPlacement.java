package codechicken.microblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.TileMultipart;

public class AdditionPlacement extends ExecutablePlacement {

    public AdditionPlacement(BlockCoord pos, Microblock part) {
        super(pos, part);
    }

    @Override
    public void place(World world, EntityPlayer player, ItemStack item) {
        TileMultipart.addPart(world, pos(), part());
    }

    @Override
    public void consume(World world, EntityPlayer player, ItemStack item) {
        item.stackSize -= 1;
    }
}
