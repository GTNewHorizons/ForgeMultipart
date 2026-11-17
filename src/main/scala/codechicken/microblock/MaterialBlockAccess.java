package codechicken.microblock;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Read-only tint lookup: substitute the material at one position, retaining the surrounding world. */
@SideOnly(Side.CLIENT)
final class MaterialBlockAccess implements IBlockAccess {

    private final IBlockAccess world;
    private final Block block;
    private final int meta, x, y, z;

    MaterialBlockAccess(IBlockAccess world, Block block, int meta, int x, int y, int z) {
        this.world = world;
        this.block = block;
        this.meta = meta;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    private boolean isOrigin(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return isOrigin(x, y, z) ? block : world.getBlock(x, y, z);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return isOrigin(x, y, z) ? meta : world.getBlockMetadata(x, y, z);
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return world.getTileEntity(x, y, z);
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int minimum) {
        return world.getLightBrightnessForSkyBlocks(x, y, z, minimum);
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        return world.isBlockProvidingPowerTo(x, y, z, side);
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return isOrigin(x, y, z) ? block.isAir(this, x, y, z) : world.isAirBlock(x, y, z);
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return world.getBiomeGenForCoords(x, z);
    }

    @Override
    public int getHeight() {
        return world.getHeight();
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return world.extendedLevelsInChunkCache();
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean fallback) {
        return world.isSideSolid(x, y, z, side, fallback);
    }
}
