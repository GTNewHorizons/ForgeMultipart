package codechicken.microblock;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Vector3;

public final class FaceMicroClass$ extends CommonMicroClass {

    public static final FaceMicroClass$ MODULE$ = new FaceMicroClass$();

    private Cuboid6[] aBounds = new Cuboid6[256];

    private FaceMicroClass$() {
        for (int side = 0; side < 6; side++) {
            Transformation transform = Rotation.sideRotations[side].at(Vector3.center);
            for (int thickness = 1; thickness < 8; thickness++) {
                double distance = thickness / 8D;
                aBounds[thickness << 4 | side] = new Cuboid6(0, 0, 0, 1, distance, 1).apply(transform);
            }
        }
    }

    public Cuboid6[] aBounds() {
        return aBounds;
    }

    public void aBounds_$eq(Cuboid6[] bounds) {
        aBounds = bounds;
    }

    @Override
    public String getName() {
        return "mcr_face";
    }

    @Override
    public int itemSlot() {
        return 3;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Class<? extends Microblock> baseTrait() {
        return (Class) FaceMicroblock.class;
    }

    @Override
    public Class<FaceMicroblockClient> clientTrait() {
        return FaceMicroblockClient.class;
    }

    @Override
    public FacePlacement$ placementProperties() {
        return FacePlacement$.MODULE$;
    }

    @Override
    public float getResistanceFactor() {
        return 1;
    }
}
