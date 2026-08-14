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

    /** Performs the test, returns true if the parts may coexist. */
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

    private static void addAll(List<Cuboid6> target, Iterable<Cuboid6> boxes) {
        for (Cuboid6 box : boxes) {
            target.add(box);
        }
    }
}
