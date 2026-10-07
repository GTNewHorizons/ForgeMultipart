package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;

class BlockMultipartCharacterizationTest {

    @Test
    void getTileOnlyReturnsNonEmptyMultipartTiles() {
        TestBlockAccess world = new TestBlockAccess();

        assertNull(BlockMultipart.getTile(world, 1, 2, 3));

        world.tile = new TileEntity();
        assertNull(BlockMultipart.getTile(world, 1, 2, 3));

        TileMultipart multipart = new TileMultipart();
        world.tile = multipart;
        assertNull(BlockMultipart.getTile(world, 1, 2, 3));

        multipart.addPart_do(new TestPart());
        assertSame(multipart, BlockMultipart.getTile(world, 1, 2, 3));
    }

    private static final class TestPart extends TMultiPart {

        @Override
        public String getType() {
            return "block_characterization";
        }
    }

    private static final class TestBlockAccess implements IBlockAccess {

        private TileEntity tile;

        @Override
        public Block getBlock(int x, int y, int z) {
            return null;
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            return tile;
        }

        @Override
        public int getLightBrightnessForSkyBlocks(int x, int y, int z, int minimum) {
            return 0;
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            return 0;
        }

        @Override
        public int isBlockProvidingPowerTo(int x, int y, int z, int direction) {
            return 0;
        }

        @Override
        public boolean isAirBlock(int x, int y, int z) {
            return false;
        }

        @Override
        public BiomeGenBase getBiomeGenForCoords(int x, int z) {
            return null;
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public boolean extendedLevelsInChunkCache() {
            return false;
        }

        @Override
        public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean defaultValue) {
            return defaultValue;
        }
    }
}
