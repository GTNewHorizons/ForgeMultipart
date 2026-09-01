package codechicken.microblock;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Scale;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Vector3;

public final class CornerMicroClass$ extends CommonMicroClass {

    public static final CornerMicroClass$ MODULE$ = new CornerMicroClass$();

    private Cuboid6[] aBounds = new Cuboid6[256];

    private CornerMicroClass$() {
        for (int corner = 0; corner < 8; corner++) {
            int scaleX = (corner & 4) == 0 ? 1 : -1;
            int scaleY = (corner & 1) == 0 ? 1 : -1;
            int scaleZ = (corner & 2) == 0 ? 1 : -1;
            Transformation transform = new Scale(new Vector3(scaleX, scaleY, scaleZ)).at(Vector3.center);

            for (int size = 1; size < 8; size++) {
                double distance = size / 8D;
                aBounds[size << 4 | corner] = new Cuboid6(0, 0, 0, distance, distance, distance).apply(transform);
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
        return "mcr_cnr";
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Class<? extends Microblock> baseTrait() {
        return (Class) CornerMicroblock.class;
    }

    @Override
    public Class<CommonMicroblockClient> clientTrait() {
        return CommonMicroblockClient.class;
    }

    @Override
    public int itemSlot() {
        return 7;
    }

    @Override
    public CornerPlacement$ placementProperties() {
        return CornerPlacement$.MODULE$;
    }

    @Override
    public float getResistanceFactor() {
        return 1;
    }
}
