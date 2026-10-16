package codechicken.multipart.examples;

import java.util.Collection;

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.NormallyOccludedPart;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

/** Compiling example for docs/api/OCCLUSION.md; this checks geometry, not full placement validity. */
public final class OcclusionExample {

    private OcclusionExample() {}

    public static boolean fitsShape(TileMultipart tile, Collection<? extends TMultiPart> parts, Cuboid6 bounds) {
        return tile.testOcclusion(parts, new NormallyOccludedPart(bounds));
    }
}
