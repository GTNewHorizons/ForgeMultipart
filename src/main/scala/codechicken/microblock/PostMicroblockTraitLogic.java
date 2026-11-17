package codechicken.microblock;

import java.util.List;

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.NormalOcclusionTest;
import codechicken.multipart.TMultiPart;
import scala.Predef$;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.Seq$;

/** Post behavior behind the retained Scala inheritance metadata and super-dispatch bridge. */
final class PostMicroblockTraitLogic {

    private PostMicroblockTraitLogic() {}

    static Cuboid6 getBounds(PostMicroblock part) {
        return PostMicroClass.aBounds()[((Microblock) part).shape()];
    }

    @SuppressWarnings("unchecked")
    static List<Cuboid6> getOcclusionBoxes(PostMicroblock part) {
        return JavaConversions.seqAsJavaList(
                (Seq<Cuboid6>) Seq$.MODULE$.apply(Predef$.MODULE$.wrapRefArray(new Cuboid6[] { part.getBounds() })));
    }

    static List<Cuboid6> getPartialOcclusionBoxes(PostMicroblock part) {
        return part.getOcclusionBoxes();
    }

    static int itemClassID(PostMicroblock part) {
        return EdgeMicroClass.getClassId();
    }

    /** Returns 0/1 for a decision, or -1 to continue through the retained Scala super accessor. */
    static int occlusionResult(PostMicroblock part, TMultiPart next) {
        if (next instanceof PostMicroblock) {
            return ((Microblock) next).getShape() != ((Microblock) part).getShape() ? 1 : 0;
        }
        if (next.getType().equals("mcr_face")) {
            if (((CommonMicroblock) next).getSlot() >> 1 == ((Microblock) part).getShape()) {
                return 1;
            }
        }
        return NormalOcclusionTest.apply(part, next) ? -1 : 0;
    }

    static float getResistanceFactor(PostMicroblock part) {
        return PostMicroClass.getResistanceFactor();
    }

    static boolean canPlaceTorchOnTop(PostMicroblock part) {
        return ((Microblock) part).getShape() == 0;
    }
}
