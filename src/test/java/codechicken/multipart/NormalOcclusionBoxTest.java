package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.examples.BoxOcclusionExample;
import scala.collection.JavaConversions;
import scala.collection.Traversable;

class NormalOcclusionBoxTest {

    private static final List<BiFunction<Iterable<Cuboid6>, Iterable<Cuboid6>, Boolean>> LEGACY = Arrays.asList(
            (left, right) -> NormalOcclusionTest.apply(scala(left), scala(right)),
            (left, right) -> NormalOcclusionTest$.MODULE$.apply(scala(left), scala(right)));

    @Test
    void legacyEntriesSnapshotBothInputsBeforeOrderedShortCircuiting() {
        LEGACY.forEach(NormalOcclusionBoxTest::assertSnapshotAndOrder);
    }

    @Test
    void legacyEntriesPropagateInputFailuresBeforeTestingGeometry() {
        LEGACY.forEach(NormalOcclusionBoxTest::assertInputFailures);
    }

    @Test
    void legacyEntriesKeepLazyNullBoxesAndOriginalGeometryFailures() {
        LEGACY.forEach(NormalOcclusionBoxTest::assertNullsAndGeometryFailures);
    }

    @Test
    void javaEntrySnapshotsSinglePassInputsBeforeOrderedShortCircuiting() {
        assertSnapshotAndOrder(NormalOcclusionTest::testBoxes);
    }

    @Test
    void javaEntryPropagatesInputFailuresBeforeTestingGeometry() {
        assertInputFailures(NormalOcclusionTest::testBoxes);
    }

    @Test
    void javaEntryKeepsLazyNullBoxesAndOriginalGeometryFailures() {
        assertNullsAndGeometryFailures(NormalOcclusionTest::testBoxes);
    }

    @Test
    void javaExampleKeepsTouchingToleranceAndOverlapRules() {
        List<Cuboid6> occupied = Collections.singletonList(new Cuboid6(0, 0, 0, 0.5, 1, 1));
        assertTrue(BoxOcclusionExample.fitsBounds(occupied, new Cuboid6(0.5, 0, 0, 1, 1, 1)));
        assertTrue(BoxOcclusionExample.fitsBounds(occupied, new Cuboid6(0.499995, 0, 0, 1, 1, 1)));
        assertFalse(BoxOcclusionExample.fitsBounds(occupied, new Cuboid6(0.49998, 0, 0, 1, 1, 1)));
        assertFalse(BoxOcclusionExample.fitsBounds(occupied, new Cuboid6(0.1, 0.1, 0.1, 0.2, 0.2, 0.2)));
        assertTrue(BoxOcclusionExample.fitsBounds(Collections.emptyList(), occupied.get(0)));
    }

    private static void assertSnapshotAndOrder(BiFunction<Iterable<Cuboid6>, Iterable<Cuboid6>, Boolean> query) {
        List<String> calls = new ArrayList<>();
        List<Cuboid6> left = new ArrayList<>();
        List<Cuboid6> right = new ArrayList<>();
        Cuboid6 first = new Cuboid6(0, 0, 0, 1, 1, 1) {

            @Override
            public boolean intersects(Cuboid6 other) {
                calls.add("first:" + other.min.x);
                left.clear();
                right.clear();
                return false;
            }
        };
        Cuboid6 last = new Cuboid6(0, 0, 0, 1, 1, 1) {

            @Override
            public boolean intersects(Cuboid6 other) {
                calls.add("last:" + other.min.x);
                return true;
            }
        };
        left.addAll(Arrays.asList(first, first, last));
        right.addAll(Arrays.asList(new Cuboid6(1, 0, 0, 2, 1, 1), new Cuboid6(2, 0, 0, 3, 1, 1)));
        assertFalse(query.apply(observed("left", left, calls), observed("right", right, calls)));
        assertEquals(
                Arrays.asList("left", "right", "first:1.0", "first:2.0", "first:1.0", "first:2.0", "last:1.0"),
                calls);
        assertTrue(left.isEmpty());
        assertTrue(right.isEmpty());
    }

    private static void assertInputFailures(BiFunction<Iterable<Cuboid6>, Iterable<Cuboid6>, Boolean> query) {
        List<String> calls = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("input failed after first box");
        Cuboid6 full = new Cuboid6(0, 0, 0, 1, 1, 1);
        Iterable<Cuboid6> failing = () -> new Iterator<Cuboid6>() {

            private boolean first = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Cuboid6 next() {
                if (!first) {
                    throw failure;
                }
                first = false;
                return full;
            }
        };
        assertSame(
                failure,
                assertThrows(
                        IllegalStateException.class,
                        () -> query.apply(failing, observed("right", Collections.singletonList(full), calls))));
        assertTrue(calls.isEmpty(), "A left collection failure must not start the right input");
        assertSame(
                failure,
                assertThrows(IllegalStateException.class, () -> query.apply(Collections.singletonList(full), failing)),
                "Collection finishes before the first overlapping pair");
        assertSame(
                failure,
                assertThrows(IllegalStateException.class, () -> query.apply(Collections.emptyList(), failing)),
                "Even an empty left side consumes the right input");
    }

    private static void assertNullsAndGeometryFailures(
            BiFunction<Iterable<Cuboid6>, Iterable<Cuboid6>, Boolean> query) {
        List<Cuboid6> empty = Collections.emptyList();
        List<Cuboid6> nullBox = Collections.singletonList(null);
        List<Cuboid6> full = Collections.singletonList(new Cuboid6(0, 0, 0, 1, 1, 1));
        assertThrows(NullPointerException.class, () -> query.apply(null, empty));
        assertThrows(NullPointerException.class, () -> query.apply(empty, null));
        assertTrue(query.apply(empty, nullBox));
        assertTrue(query.apply(nullBox, empty));
        assertThrows(NullPointerException.class, () -> query.apply(nullBox, full));
        assertThrows(NullPointerException.class, () -> query.apply(full, nullBox));
        assertFalse(query.apply(full, Arrays.asList(full.get(0), null)));
        IllegalArgumentException failure = new IllegalArgumentException("intersects failed");
        Cuboid6 broken = new Cuboid6(0, 0, 0, 1, 1, 1) {

            @Override
            public boolean intersects(Cuboid6 other) {
                throw failure;
            }
        };
        assertSame(
                failure,
                assertThrows(
                        IllegalArgumentException.class,
                        () -> query.apply(Collections.singletonList(broken), full)));
    }

    private static Iterable<Cuboid6> observed(String name, List<Cuboid6> boxes, List<String> calls) {
        return () -> {
            assertFalse(calls.contains(name), "Input iterator must be requested only once");
            calls.add(name);
            return boxes.iterator();
        };
    }

    private static Traversable<Cuboid6> scala(Iterable<Cuboid6> boxes) {
        if (boxes == null) {
            return null;
        }
        return JavaConversions.iterableAsScalaIterable(boxes);
    }
}
