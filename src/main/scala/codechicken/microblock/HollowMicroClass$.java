package codechicken.microblock;

import java.util.Arrays;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Vector3;
import scala.collection.JavaConversions;
import scala.collection.Seq;

public final class HollowMicroClass$ extends CommonMicroClass {

    public static final HollowMicroClass$ MODULE$ = new HollowMicroClass$();

    private Seq<Cuboid6>[] pBoxes;
    private Cuboid6[] occBounds;

    @SuppressWarnings("unchecked")
    private HollowMicroClass$() {
        pBoxes = (Seq<Cuboid6>[]) new Seq<?>[256];
        occBounds = new Cuboid6[256];
        for (int side = 0; side < 6; side++) {
            Transformation transform = Rotation.sideRotations[side].at(Vector3.center);
            for (int size = 1; size < 8; size++) {
                double thickness = size / 8D;
                double width = 1 / 8D;
                int shape = size << 4 | side;
                pBoxes[shape] = JavaConversions
                        .asScalaBuffer(
                                Arrays.asList(
                                        new Cuboid6(0, 0, 0, width, thickness, 1).apply(transform),
                                        new Cuboid6(1 - width, 0, 0, 1, thickness, 1).apply(transform),
                                        new Cuboid6(width, 0, 0, 1 - width, thickness, width).apply(transform),
                                        new Cuboid6(width, 0, 1 - width, 1 - width, thickness, 1).apply(transform)))
                        .toList();
                occBounds[shape] = new Cuboid6(1 / 8D, 0, 1 / 8D, 7 / 8D, thickness, 7 / 8D).apply(transform);
            }
        }
    }

    public Seq<Cuboid6>[] pBoxes() {
        return pBoxes;
    }

    public void pBoxes_$eq(Seq<Cuboid6>[] bounds) {
        pBoxes = bounds;
    }

    public Cuboid6[] occBounds() {
        return occBounds;
    }

    public void occBounds_$eq(Cuboid6[] bounds) {
        occBounds = bounds;
    }

    @Override
    public String getName() {
        return "mcr_hllw";
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Class<? extends Microblock> baseTrait() {
        return (Class) HollowMicroblock.class;
    }

    @Override
    public Class<HollowMicroblockClient> clientTrait() {
        return HollowMicroblockClient.class;
    }

    @Override
    public int itemSlot() {
        return 3;
    }

    @Override
    public HollowPlacement$ placementProperties() {
        return HollowPlacement$.MODULE$;
    }

    @Override
    public float getResistanceFactor() {
        return 1;
    }
}
