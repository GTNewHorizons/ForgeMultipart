package codechicken.microblock;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Vector3;

public final class PostMicroClass$ extends MicroblockClass {

    public static final PostMicroClass$ MODULE$ = new PostMicroClass$();

    private Cuboid6[] aBounds = new Cuboid6[256];

    private PostMicroClass$() {
        for (int axis = 0; axis < 3; axis++) {
            Transformation transform = Rotation.sideRotations[axis << 1].at(Vector3.center);
            for (int size = 2; size < 8; size += 2) {
                double min = 0.5 - size / 16D;
                double max = 0.5 + size / 16D;
                aBounds[size << 4 | axis] = new Cuboid6(min, 0, min, max, 1, max).apply(transform);
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
        return "mcr_post";
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Class<? extends Microblock> baseTrait() {
        return (Class) PostMicroblock.class;
    }

    @Override
    public Class<PostMicroblockClient> clientTrait() {
        return PostMicroblockClient.class;
    }

    @Override
    public float getResistanceFactor() {
        return 0.5F;
    }
}
