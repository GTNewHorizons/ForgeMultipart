package codechicken.multipart;

import codechicken.lib.vec.Cuboid6;
import scala.collection.Traversable;

/**
 * Scala companion singleton. Retained because compiled Scala consumers read MODULE$ and call these instance methods.
 */
public final class NormalOcclusionTest$ {

    public static final NormalOcclusionTest$ MODULE$ = new NormalOcclusionTest$();

    private NormalOcclusionTest$() {}

    public boolean apply(Traversable<Cuboid6> boxes1, Traversable<Cuboid6> boxes2) {
        return NormalOcclusionTest.apply(boxes1, boxes2);
    }

    public boolean apply(JNormalOcclusion part1, TMultiPart part2) {
        return NormalOcclusionTest.apply(part1, part2);
    }
}
