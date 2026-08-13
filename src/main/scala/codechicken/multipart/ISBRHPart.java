package codechicken.multipart;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

public interface ISBRHPart 
{
    boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, RenderBlocks renderer);
}
