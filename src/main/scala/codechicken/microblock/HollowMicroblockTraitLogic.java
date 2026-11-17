package codechicken.microblock;

import java.util.List;

import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.NormalOcclusionTest;
import codechicken.multipart.TMultiPart;
import scala.MatchError;
import scala.Predef$;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.Seq$;
import scala.collection.TraversableLike;
import scala.collection.mutable.Buffer;
import scala.collection.mutable.Buffer$;
import scala.runtime.AbstractFunction1;

/** Hollow-cover behavior behind the retained Scala inheritance metadata and super-dispatch bridge. */
final class HollowMicroblockTraitLogic {

    private HollowMicroblockTraitLogic() {}

    static Cuboid6 getBounds(HollowMicroblock part) {
        return FaceMicroClass.aBounds()[((Microblock) part).shape()];
    }

    static boolean normalOcclusionTest(HollowMicroblock part, TMultiPart next) {
        return NormalOcclusionTest.apply(part, next);
    }

    static List<Cuboid6> getPartialOcclusionBoxes(HollowMicroblock part) {
        return JavaConversions.seqAsJavaList(HollowMicroClass$.MODULE$.pBoxes()[((Microblock) part).shape()]);
    }

    static int getHollowSize(HollowMicroblock part) {
        if (((TMultiPart) part).tile() == null) {
            return 8;
        }
        // The second virtual lookup is observable when a part overrides tile().
        TMultiPart center = ((TMultiPart) part).tile().partMap(6);
        if (center instanceof ISidedHollowConnect) {
            return ((ISidedHollowConnect) center).getHollowSize(part.getSlot());
        }
        return 8;
    }

    static Iterable<Cuboid6> getOcclusionBoxes(HollowMicroblock part) {
        int size = part.getHollowSize();
        Cuboid6 c = HollowMicroClass$.MODULE$.occBounds()[((Microblock) part).shape()];
        double d1 = 0.5 - size / 32D;
        double d2 = 0.5 + size / 32D;
        double x1 = c.min.x;
        double x2 = c.max.x;
        double y1 = c.min.y;
        double y2 = c.max.y;
        double z1 = c.min.z;
        double z2 = c.max.z;
        int slot = part.getSlot();
        Cuboid6[] boxes;
        switch (slot) {
            case 0:
            case 1:
                boxes = new Cuboid6[] { new Cuboid6(d2, y1, d1, x2, y2, d2), new Cuboid6(x1, y1, d1, d1, y2, d2),
                        new Cuboid6(x1, y1, d2, x2, y2, z2), new Cuboid6(x1, y1, z1, x2, y2, d1) };
                break;
            case 2:
            case 3:
                boxes = new Cuboid6[] { new Cuboid6(d1, d2, z1, d2, y2, z2), new Cuboid6(d1, y1, z1, d2, d1, z2),
                        new Cuboid6(d2, y1, z1, x2, y2, z2), new Cuboid6(x1, y1, z1, d1, y2, z2) };
                break;
            case 4:
            case 5:
                boxes = new Cuboid6[] { new Cuboid6(x1, d1, d2, x2, d2, z2), new Cuboid6(x1, d1, z1, x2, d2, d1),
                        new Cuboid6(x1, d2, z1, x2, y2, z2), new Cuboid6(x1, y1, z1, x2, d1, z2) };
                break;
            default:
                throw new MatchError(slot);
        }
        return JavaConversions.seqAsJavaList(sequence(boxes));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static List<Cuboid6> getCollisionBoxes(HollowMicroblock part) {
        int size = part.getHollowSize();
        double d1 = 0.5 - size / 32D;
        double d2 = 0.5 + size / 32D;
        double thickness = (((Microblock) part).shape() >> 4) / 8D;
        Transformation rotation = Rotation.sideRotations[((Microblock) part).shape() & 15].at(Vector3.center);
        Seq<Cuboid6> boxes = sequence(
                new Cuboid6[] { new Cuboid6(0, 0, 0, 1, thickness, d1), new Cuboid6(0, 0, d2, 1, thickness, 1),
                        new Cuboid6(0, 0, d1, d1, thickness, d2), new Cuboid6(d2, 0, d1, 1, thickness, d2) });
        return JavaConversions.seqAsJavaList(
                (Seq<Cuboid6>) ((TraversableLike) boxes).map(new RotateBox(rotation), Seq$.MODULE$.canBuildFrom()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static List<IndexedCuboid6> getSubParts(HollowMicroblock part) {
        return JavaConversions.bufferAsJavaList(
                (Buffer<IndexedCuboid6>) ((TraversableLike) JavaConversions.asScalaBuffer(part.getCollisionBoxes()))
                        .map(new IndexBox(), Buffer$.MODULE$.canBuildFrom()));
    }

    @SuppressWarnings("unchecked")
    private static Seq<Cuboid6> sequence(Cuboid6[] boxes) {
        return (Seq<Cuboid6>) Seq$.MODULE$.apply(Predef$.MODULE$.wrapRefArray(boxes));
    }

    private static final class RotateBox extends AbstractFunction1<Cuboid6, Cuboid6> implements scala.Serializable {

        private static final long serialVersionUID = 0L;
        private final Transformation rotation;

        RotateBox(Transformation rotation) {
            this.rotation = rotation;
        }

        @Override
        public Cuboid6 apply(Cuboid6 box) {
            return box.apply(rotation);
        }
    }

    private static final class IndexBox extends AbstractFunction1<Cuboid6, IndexedCuboid6>
            implements scala.Serializable {

        private static final long serialVersionUID = 0L;

        IndexBox() {}

        @Override
        public IndexedCuboid6 apply(Cuboid6 box) {
            return new IndexedCuboid6(0, box);
        }
    }
}
