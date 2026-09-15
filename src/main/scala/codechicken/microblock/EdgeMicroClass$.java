package codechicken.microblock;

import codechicken.lib.vec.AxisCycle;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Scale;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.TransformationList;
import codechicken.lib.vec.Vector3;

public final class EdgeMicroClass$ extends CommonMicroClass {

    public static final EdgeMicroClass$ MODULE$ = new EdgeMicroClass$();

    private Cuboid6[] aBounds = new Cuboid6[256];

    private EdgeMicroClass$() {
        for (int edge = 0; edge < 12; edge++) {
            int scaleX = (edge & 2) == 0 ? 1 : -1;
            int scaleZ = (edge & 1) == 0 ? 1 : -1;
            Transformation transform = new TransformationList(
                    new Scale(new Vector3(scaleX, 1, scaleZ)),
                    AxisCycle.cycles[edge >> 2]).at(Vector3.center);

            for (int size = 1; size < 8; size++) {
                double distance = size / 8D;
                aBounds[size << 4 | edge] = new Cuboid6(0, 0, 0, distance, 1, distance).apply(transform);
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
    public int itemSlot() {
        return 15;
    }

    @Override
    public String getName() {
        return "mcr_edge";
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Class<? extends Microblock> baseTrait() {
        return (Class) EdgeMicroblock.class;
    }

    @Override
    public Class<CommonMicroblockClient> clientTrait() {
        return CommonMicroblockClient.class;
    }

    @Override
    public EdgePlacement$ placementProperties() {
        return EdgePlacement$.MODULE$;
    }

    @Override
    public float getResistanceFactor() {
        return 0.5F;
    }
}
