package codechicken.multipart;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

public interface JsonModeledPart {

    Block getBlock();

    /**
     * Use to give your part a world that can be used to define properties for rendering. For example having your world
     * returning your block with some specific state such as meta you can override the getMeta for the input location to
     * be the meta you desire
     * 
     * @return The world to use
     */
    IBlockAccess getRenderWorld();
}
