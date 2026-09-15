package codechicken.microblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import codechicken.lib.vec.BlockCoord;

public abstract class ExecutablePlacement {

    private final BlockCoord pos;
    private final Microblock part;

    public ExecutablePlacement(BlockCoord pos, Microblock part) {
        this.pos = pos;
        this.part = part;
    }

    public BlockCoord pos() {
        return pos;
    }

    public Microblock part() {
        return part;
    }

    public abstract void place(World world, EntityPlayer player, ItemStack item);

    public abstract void consume(World world, EntityPlayer player, ItemStack item);
}
