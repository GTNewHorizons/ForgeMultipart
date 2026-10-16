package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;

import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.multipart.examples.HitPartExample;
import scala.Tuple2;

class BlockMultipartCharacterizationTest {

    private static final Vec3 ORIGIN = Vec3.createVectorHelper(0, 0, 0);

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

    @Test
    void reduceMOPReturnsTheBoxedPartIndexAndARebasedHit() {
        TileMultipart tile = hitTile();
        ExtendedMOP hit = tile.collisionRayTrace(ORIGIN, ORIGIN);

        List<Tuple2<Object, ExtendedMOP>> results = Arrays
                .asList(BlockMultipart.reduceMOP(hit), BlockMultipart$.MODULE$.reduceMOP(hit));
        for (Tuple2<Object, ExtendedMOP> reduced : results) {
            // GT5U 5.09.54.205 casts _1() straight to Integer and indexes jPartList() with it.
            assertEquals(Integer.class, reduced._1().getClass());
            assertEquals(2, (int) (Integer) reduced._1());
            assertNotSame(hit, reduced._2());
            assertEquals("near-data", reduced._2().data);
            assertEquals(hit.dist, reduced._2().dist);
            assertEquals(hit.blockX, reduced._2().blockX);
        }
    }

    @Test
    void hitPartIndexMatchesReduceMOPForEveryHit() {
        TileMultipart tile = hitTile();

        int hits = 0;
        for (ExtendedMOP hit : tile.rayTraceAll(ORIGIN, ORIGIN)) {
            int index = BlockMultipart.hitPartIndex(hit);
            assertEquals(BlockMultipart.reduceMOP(hit)._1(), index);
            assertSame(((HitPart) tile.jPartList().get(index)).produced, hit);
            assertSame(tile.jPartList().get(index), HitPartExample.struckPart(tile, hit));
            hits++;
        }
        assertEquals(2, hits);
    }

    @Test
    void hitPartIndexIsMinusOneWhenTheHitCarriesNoPartIndex() {
        Tuple2<Object, Object> nonIntegerIndex = new Tuple2<>("0", null);
        for (Object data : new Object[] { null, "subhit", 3, nonIntegerIndex }) {
            assertEquals(-1, BlockMultipart.hitPartIndex(new ExtendedMOP(0, 0, 0, 0, ORIGIN, data)));
        }
        assertEquals(-1, BlockMultipart.hitPartIndex(new MovingObjectPosition(0, 0, 0, 0, ORIGIN)));
        assertEquals(-1, BlockMultipart.hitPartIndex(null));
        assertNull(HitPartExample.struckPart(new TileMultipart(), null));
    }

    @Test
    void exampleRejectsAnIndexOutsideTheTile() {
        TileMultipart tile = hitTile();
        ExtendedMOP hit = tile.collisionRayTrace(ORIGIN, ORIGIN);
        TileMultipart smaller = new TileMultipart();
        smaller.addPart_do(new TestPart());

        assertNull(HitPartExample.struckPart(smaller, hit));
    }

    /** Parts 0 and 2 are hit, part 1 misses; part 2 is nearest. */
    private static TileMultipart hitTile() {
        TileMultipart tile = new TileMultipart();
        tile.addPart_do(new HitPart(9d, "far-data"));
        tile.addPart_do(new HitPart(null, null));
        tile.addPart_do(new HitPart(1d, "near-data"));
        return tile;
    }

    private static final class TestPart extends TMultiPart {

        @Override
        public String getType() {
            return "block_characterization";
        }
    }

    private static final class HitPart extends TMultiPart {

        private final Double dist;
        private final Object data;
        private ExtendedMOP produced;

        private HitPart(Double dist, Object data) {
            this.dist = dist;
            this.data = data;
        }

        @Override
        public String getType() {
            return "block_characterization_hit";
        }

        @Override
        public ExtendedMOP collisionRayTrace(Vec3 start, Vec3 end) {
            if (dist == null) {
                return null;
            }
            produced = new ExtendedMOP(0, 0, 0, 0, ORIGIN, data);
            produced.dist = dist;
            return produced;
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
