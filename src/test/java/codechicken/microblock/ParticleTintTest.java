package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.material.Material;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;

import org.junit.jupiter.api.Test;

class ParticleTintTest {

    @Test
    void leafVariantsUseMaterialMetadataInsteadOfMultipartMetadata() {
        IBlockAccess host = access((method, args) -> {
            if (method.equals("getBlockMetadata")) return 0;
            throw new AssertionError("Fixed leaf tints must not read the biome: " + method);
        });
        Block leaves = new BlockOldLeaf();
        assertEquals(
                ColorizerFoliage.getFoliageColorPine(),
                new BlockMicroMaterial(leaves, 1).getBreakingColour(1, host, 10, 64, 20));
        assertEquals(
                ColorizerFoliage.getFoliageColorBirch(),
                new BlockMicroMaterial(leaves, 2).getBreakingColour(1, host, 10, 64, 20));
    }

    @Test
    void tintSeesMaterialAtOriginAndRealNeighborsAndBiome() {
        Block neighbor = new Block(Material.rock) {};
        IBlockAccess host = access((method, args) -> {
            if (method.equals("getBiomeGenForCoords")) {
                assertEquals(10, args[0]);
                assertEquals(20, args[1]);
                return BiomeGenBase.plains;
            }
            assertEquals(11, args[0]);
            assertEquals(64, args[1]);
            assertEquals(20, args[2]);
            if (method.equals("getBlock")) return neighbor;
            if (method.equals("getBlockMetadata")) return 7;
            throw new AssertionError(method);
        });
        Block tinted = new Block(Material.rock) {

            @Override
            public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
                assertSame(this, world.getBlock(x, y, z));
                assertEquals(2, world.getBlockMetadata(x, y, z));
                assertSame(neighbor, world.getBlock(x + 1, y, z));
                assertEquals(7, world.getBlockMetadata(x + 1, y, z));
                assertSame(BiomeGenBase.plains, world.getBiomeGenForCoords(x, z));
                return 0x123456;
            }
        };
        assertEquals(0x123456, new BlockMicroMaterial(tinted, 2).getBreakingColour(1, host, 10, 64, 20));
    }

    @Test
    void grassTintsOnlyItsTopFace() {
        Block tinted = new Block(Material.grass) {

            @Override
            public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
                return 0x123456;
            }
        };
        GrassMicroMaterial grass = new GrassMicroMaterial() {

            @Override
            public Block block() {
                return tinted;
            }
        };
        for (int side = 0; side < 6; side++) {
            assertEquals(side == 1 ? 0x123456 : 0xFFFFFF, grass.getBreakingColour(side, null, 0, 0, 0));
        }
    }

    private static IBlockAccess access(java.util.function.BiFunction<String, Object[], Object> callback) {
        return (IBlockAccess) Proxy.newProxyInstance(
                IBlockAccess.class.getClassLoader(),
                new Class<?>[] { IBlockAccess.class },
                (proxy, method, args) -> callback.apply(method.getName(), args));
    }
}
