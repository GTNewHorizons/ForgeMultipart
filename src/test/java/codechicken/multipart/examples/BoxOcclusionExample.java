package codechicken.multipart.examples;

import java.util.Collections;

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.NormalOcclusionTest;

/** Compiling example for docs/api/OCCLUSION.md; supplied bounds do not imply full placement approval. */
public final class BoxOcclusionExample {

    private BoxOcclusionExample() {}

    public static boolean fitsBounds(Iterable<? extends Cuboid6> occupied, Cuboid6 candidate) {
        return NormalOcclusionTest.testBoxes(occupied, Collections.singletonList(candidate));
    }
}
