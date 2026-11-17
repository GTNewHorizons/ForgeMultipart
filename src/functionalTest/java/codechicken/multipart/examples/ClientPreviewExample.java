package codechicken.multipart.examples;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import codechicken.multipart.MultipartHelper;
import codechicken.multipart.MultipartRenderer;
import codechicken.multipart.TileMultipart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Compiling example for constructing and rendering an uninstalled multipart preview without reflection or Scala. */
@SideOnly(Side.CLIENT)
public final class ClientPreviewExample {

    private ClientPreviewExample() {}

    public static TileMultipart create(World clientWorld, NBTTagCompound savedTile) {
        return MultipartHelper.createPreviewTileFromNBT(clientWorld, savedTile);
    }

    public static boolean render(IBlockAccess world, int x, int y, int z, Block multipartBlock, RenderBlocks renderer) {
        return MultipartRenderer
                .renderWorldBlock(world, x, y, z, multipartBlock, multipartBlock.getRenderType(), renderer);
    }
}
