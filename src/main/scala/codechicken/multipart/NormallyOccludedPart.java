package codechicken.multipart;

import java.util.Collections;

import codechicken.lib.vec.Cuboid6;

/** Utility part class for performing 3rd party occlusion tests. */
public class NormallyOccludedPart extends TMultiPart implements TNormalOcclusion {

    private final Iterable<Cuboid6> bounds;

    public NormallyOccludedPart(Iterable<Cuboid6> bounds) {
        this.bounds = bounds;
    }

    public NormallyOccludedPart(Cuboid6 bound) {
        this(Collections.singletonList(bound));
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public Iterable<Cuboid6> getOcclusionBoxes() {
        return bounds;
    }

    @Override
    public boolean occlusionTest(TMultiPart npart) {
        return NormalOcclusionTest.apply(this, npart) && super.occlusionTest(npart);
    }
}
