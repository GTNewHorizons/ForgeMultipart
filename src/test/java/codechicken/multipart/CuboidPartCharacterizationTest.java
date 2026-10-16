package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;

class CuboidPartCharacterizationTest {

    @Test
    void subPartsHoldOneIndexedCopyOfTheBounds() {
        TestPart part = new TestPart(box(0, 0, 0, 0.5, 0.5, 0.5));

        List<IndexedCuboid6> subParts = drain(part.getSubParts());

        assertEquals(1, subParts.size());
        IndexedCuboid6 only = assertInstanceOf(IndexedCuboid6.class, subParts.get(0));
        assertEquals(Integer.valueOf(0), only.data);
        assertGeometry(part.getBounds(), only);
        assertNotSame(part.getBounds(), only);
    }

    @Test
    void collisionBoxesAliasTheBoundsInstance() {
        TestPart part = new TestPart(box(0, 0, 0, 1, 1, 1));

        List<Cuboid6> boxes = drain(part.getCollisionBoxes());

        assertEquals(1, boxes.size());
        assertSame(part.getBounds(), boxes.get(0));
    }

    @Test
    void resultsFollowTheCurrentBounds() {
        TestPart part = new TestPart(box(0, 0, 0, 1, 1, 1));
        part.bounds = box(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);

        assertGeometry(part.getBounds(), drain(part.getSubParts()).get(0));
        assertSame(part.getBounds(), drain(part.getCollisionBoxes()).get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deprecatedBridgeMatchesTheInstanceMethods() {
        TestPart part = new TestPart(box(0.1, 0.2, 0.3, 0.4, 0.5, 0.6));

        List<IndexedCuboid6> bridgeSubParts = drain(TCuboidPart$class.getSubParts(part));
        List<Cuboid6> bridgeBoxes = drain(TCuboidPart$class.getCollisionBoxes(part));

        assertEquals(1, bridgeSubParts.size());
        assertEquals(Integer.valueOf(0), bridgeSubParts.get(0).data);
        assertGeometry(part.getBounds(), bridgeSubParts.get(0));
        assertEquals(1, bridgeBoxes.size());
        assertSame(part.getBounds(), bridgeBoxes.get(0));
    }

    /**
     * TMultiPart supplies empty defaults. A cuboid part must not fall back to them: on the JVM a superclass method
     * beats an interface default, so this is the guard against the port silently losing the cuboid behavior.
     */
    @Test
    void cuboidBehaviorWinsOverTheEmptyTMultiPartDefaults() {
        TMultiPart bare = new TMultiPart() {

            @Override
            public String getType() {
                return "test:bare";
            }
        };

        assertFalse(bare.getSubParts().iterator().hasNext());
        assertFalse(bare.getCollisionBoxes().iterator().hasNext());

        TestPart part = new TestPart(box(0, 0, 0, 1, 1, 1));
        assertTrue(part.getSubParts().iterator().hasNext());
        assertTrue(part.getCollisionBoxes().iterator().hasNext());
    }

    private static void assertGeometry(Cuboid6 expected, Cuboid6 actual) {
        assertEquals(expected.min.x, actual.min.x);
        assertEquals(expected.min.y, actual.min.y);
        assertEquals(expected.min.z, actual.min.z);
        assertEquals(expected.max.x, actual.max.x);
        assertEquals(expected.max.y, actual.max.y);
        assertEquals(expected.max.z, actual.max.z);
    }

    private static <T> List<T> drain(Iterable<T> values) {
        List<T> result = new ArrayList<>();
        for (Iterator<T> iterator = values.iterator(); iterator.hasNext();) {
            result.add(iterator.next());
        }
        return result;
    }

    private static Cuboid6 box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new Cuboid6(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static final class TestPart extends JCuboidPart {

        private Cuboid6 bounds;

        private TestPart(Cuboid6 bounds) {
            this.bounds = bounds;
        }

        @Override
        public Cuboid6 getBounds() {
            return bounds;
        }

        @Override
        public String getType() {
            return "test:cuboid";
        }
    }
}
