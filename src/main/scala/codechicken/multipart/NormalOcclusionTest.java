package codechicken.multipart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import codechicken.lib.vec.Cuboid6;
import scala.collection.JavaConversions;
import scala.collection.Traversable;

/**
 * Simple bounding box based occlusion testing. If any two parts have overlapping bounding boxes, the test fails.
 *
 * @see TIconHitEffects for notes on the Scala/Java composition setup.
 */
public final class NormalOcclusionTest {

    private NormalOcclusionTest() {}

    /**
     * Tests two groups of boxes, returning true if no cross-group pair intersects according to
     * {@link Cuboid6#intersects(Cuboid6)}, including its existing tolerance for touching bounds.
     *
     * <p>
     * Fully consumes the first input, then the second, once each, before testing any pairs. These shallow snapshots
     * preserve order, duplicates, null entries and box identities. The first input is the outer loop; the first
     * intersection returns false. Input and intersection exceptions propagate unchanged. Null inputs fail during
     * copying; null entries fail only if an intersection call uses them. Empty inputs allow all boxes in the other
     * group, but both inputs are still consumed.
     *
     * <p>
     * This only tests supplied geometry. It does not invoke part callbacks, perform aggregate partial-occlusion checks
     * or approve placement. Boxes remain shared and mutable; changing their coordinates can affect the query.
     */
    public static boolean testBoxes(Iterable<? extends Cuboid6> boxes1, Iterable<? extends Cuboid6> boxes2) {
        List<Cuboid6> first = new ArrayList<>();
        addAll(first, boxes1);
        List<Cuboid6> second = new ArrayList<>();
        addAll(second, boxes2);
        return test(first, second);
    }

    /**
     * Performs the test, returns true if the parts may coexist.
     *
     * @deprecated Use {@link #testBoxes(Iterable, Iterable)} with Java iterables. Retained for existing Scala callers.
     */
    @Deprecated
    public static boolean apply(Traversable<Cuboid6> boxes1, Traversable<Cuboid6> boxes2) {
        return test(collect(boxes1), collect(boxes2));
    }

    /** Performs the test, returns true if the parts may coexist. */
    public static boolean apply(JNormalOcclusion part1, TMultiPart part2) {
        List<Cuboid6> boxes = new ArrayList<>();
        if (part2 instanceof JNormalOcclusion) {
            addAll(boxes, ((JNormalOcclusion) part2).getOcclusionBoxes());
        }
        if (part2 instanceof JPartialOcclusion) {
            addAll(boxes, ((JPartialOcclusion) part2).getPartialOcclusionBoxes());
        }
        return test(boxes, part1.getOcclusionBoxes());
    }

    private static boolean test(Iterable<Cuboid6> boxes1, Iterable<Cuboid6> boxes2) {
        for (Cuboid6 v1 : boxes1) {
            for (Cuboid6 v2 : boxes2) {
                if (v1.intersects(v2)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<Cuboid6> collect(Traversable<Cuboid6> boxes) {
        List<Cuboid6> result = new ArrayList<>();
        Iterator<Cuboid6> iterator = JavaConversions.asJavaIterator(boxes.toIterator());
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    private static void addAll(List<Cuboid6> target, Iterable<? extends Cuboid6> boxes) {
        for (Cuboid6 box : boxes) {
            target.add(box);
        }
    }
}
