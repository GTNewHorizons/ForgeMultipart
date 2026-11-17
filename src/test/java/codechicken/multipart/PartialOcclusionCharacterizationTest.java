package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;

class PartialOcclusionCharacterizationTest {

    @Test
    void hasFixedGridAndIgnoresUnfilledEntries() {
        PartialOcclusionTest test = new PartialOcclusionTest(2);

        assertEquals(8, test.res());
        assertEquals(8 * 8 * 8, test.bits().length);
        assertEquals(2, test.partial().length);
        assertTrue(test.apply());
    }

    @Test
    void mapsPartIdsToXMajorVoxels() {
        PartialOcclusionTest test = new PartialOcclusionTest(1);
        test.fill(0, Collections.singletonList(voxel(2, 3, 4)), false);

        assertTrue(test.partial()[0]);
        assertEquals(1, test.bits()[index(2, 3, 4)]);
        assertEquals(1, occupiedCells(test));
        assertTrue(test.apply());
    }

    @Test
    void roundsBoxCoordinatesAtHalfVoxel() {
        PartialOcclusionTest belowThreshold = new PartialOcclusionTest(1);
        belowThreshold.fill(0, Collections.singletonList(box(0, 0, 0, 0.06249, 0.06249, 0.06249)), false);

        PartialOcclusionTest atThreshold = new PartialOcclusionTest(1);
        atThreshold.fill(0, Collections.singletonList(box(0, 0, 0, 0.0625, 0.0625, 0.0625)), false);

        assertFalse(belowThreshold.apply());
        assertTrue(atThreshold.apply());
    }

    @Test
    void requiredPartWithoutVisibleVoxelsFails() {
        PartialOcclusionTest test = new PartialOcclusionTest(1);
        test.fill(0, Collections.emptyList(), false);

        assertTrue(test.partial()[0]);
        assertFalse(test.apply());
    }

    @Test
    void completeOcclusionFlagExemptsPartFromVisibility() {
        PartialOcclusionTest test = new PartialOcclusionTest(1);
        test.fill(0, Collections.emptyList(), true);

        assertFalse(test.partial()[0]);
        assertTrue(test.apply());
    }

    @Test
    void javaInterfaceDefaultsToRequiredVisibility() {
        JPartialOcclusion part = Collections::emptyList;

        assertFalse(part.allowCompleteOcclusion());
    }

    @Test
    void fullyOverlappingPartsLoseTheirVisibleVoxel() {
        PartialOcclusionTest test = new PartialOcclusionTest(2);
        test.fill(0, Collections.singletonList(voxel(0, 0, 0)), false);
        test.fill(1, Collections.singletonList(voxel(0, 0, 0)), false);

        assertEquals(-1, test.bits()[index(0, 0, 0)]);
        assertFalse(test.apply());
    }

    @Test
    void everyRequiredPartNeedsAnExclusiveVoxel() {
        PartialOcclusionTest bothVisible = new PartialOcclusionTest(2);
        bothVisible.fill(0, Collections.singletonList(voxels(0, 0, 0, 2, 1, 1)), false);
        bothVisible.fill(1, Collections.singletonList(voxels(1, 0, 0, 3, 1, 1)), false);

        PartialOcclusionTest secondCovered = new PartialOcclusionTest(2);
        secondCovered.fill(0, Collections.singletonList(voxels(0, 0, 0, 2, 1, 1)), false);
        secondCovered.fill(1, Collections.singletonList(voxel(1, 0, 0)), false);

        assertTrue(bothVisible.apply());
        assertFalse(secondCovered.apply());
    }

    @Test
    void overlappingBoxesFromOnePartInvalidateSharedVoxels() {
        PartialOcclusionTest test = new PartialOcclusionTest(1);
        Cuboid6 box = voxel(0, 0, 0);
        test.fill(0, Arrays.asList(box, box.copy()), false);

        assertEquals(-1, test.bits()[index(0, 0, 0)]);
        assertFalse(test.apply());
    }

    @Test
    void partOverloadUsesBoxesAndCompleteOcclusionFlag() {
        PartialOcclusionTest visible = new PartialOcclusionTest(1);
        visible.fill(0, new TestPart(false, voxel(0, 0, 0)));

        PartialOcclusionTest missing = new PartialOcclusionTest(1);
        missing.fill(0, new TestPart(false));

        PartialOcclusionTest exempt = new PartialOcclusionTest(1);
        exempt.fill(0, new TestPart(true));

        assertTrue(visible.apply());
        assertFalse(missing.apply());
        assertTrue(exempt.apply());
    }

    private static int occupiedCells(PartialOcclusionTest test) {
        int occupied = 0;
        for (byte value : test.bits()) {
            if (value != 0) {
                occupied++;
            }
        }
        return occupied;
    }

    private static int index(int x, int y, int z) {
        return (x * 8 + y) * 8 + z;
    }

    private static Cuboid6 voxel(int x, int y, int z) {
        return voxels(x, y, z, x + 1, y + 1, z + 1);
    }

    private static Cuboid6 voxels(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return box(minX / 8d, minY / 8d, minZ / 8d, maxX / 8d, maxY / 8d, maxZ / 8d);
    }

    private static Cuboid6 box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new Cuboid6(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static final class TestPart implements JPartialOcclusion {

        private final boolean allowCompleteOcclusion;
        private final Iterable<Cuboid6> boxes;

        private TestPart(boolean allowCompleteOcclusion, Cuboid6... boxes) {
            this.allowCompleteOcclusion = allowCompleteOcclusion;
            this.boxes = Collections.unmodifiableList(Arrays.asList(boxes));
        }

        @Override
        public Iterable<Cuboid6> getPartialOcclusionBoxes() {
            return boxes;
        }

        @Override
        public boolean allowCompleteOcclusion() {
            return allowCompleteOcclusion;
        }
    }
}
