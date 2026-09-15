package codechicken.microblock;

import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.multipart.TMultiPart;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

/** Client behavior behind the retained Scala state, inheritance and lifecycle bridges. */
final class PostMicroblockClientLogic {

    private PostMicroblockClientLogic() {}

    static void render(PostMicroblockClient part, Vector3 pos, int pass) {
        IMicroMaterial material = ((Microblock) part).getIMaterial();
        if (pass == -1) {
            MicroblockRender.renderCuboid(pos, material, pass, part.getBounds(), 0);
        } else {
            MicroblockRender.renderCuboid(pos, material, pass, part.renderBounds1(), 0);
            if (part.renderBounds2() != null) {
                MicroblockRender.renderCuboid(pos, material, pass, part.renderBounds2(), 0);
            }
        }
    }

    static void recalcBounds(PostMicroblockClient part) {
        part.renderBounds1_$eq(part.getBounds().copy());
        part.renderBounds2_$eq(null);
        part.shrinkFace(((Microblock) part).getShape() << 1);
        part.shrinkFace(((Microblock) part).getShape() << 1 | 1);
        ((TMultiPart) part).tile().partList().foreach(new ShrinkPosts(part));
    }

    static void shrinkFace(PostMicroblockClient part, int side) {
        TMultiPart other = ((TMultiPart) part).tile().partMap(side);
        if (other != null && other.getType().equals("mcr_face")) {
            MicroOcclusion.shrink(part.renderBounds1(), ((CommonMicroblock) other).getBounds(), side);
        }
    }

    static void shrinkPost(PostMicroblockClient part, PostMicroblock other) {
        // Scala equality calls the left operand's equals even when the references are identical.
        if (other == null ? part == null : other.equals(part)) {
            return;
        }
        if (part.thisShrinks(other)) {
            if (part.renderBounds2() == null) {
                part.renderBounds2_$eq(part.getBounds().copy());
            }
            MicroOcclusion.shrink(part.renderBounds1(), other.getBounds(), ((Microblock) part).getShape() << 1 | 1);
            MicroOcclusion.shrink(part.renderBounds2(), other.getBounds(), ((Microblock) part).getShape() << 1);
        }
    }

    static boolean thisShrinks(PostMicroblockClient part, PostMicroblock other) {
        if (((Microblock) part).getSize() != ((Microblock) other).getSize()) {
            return ((Microblock) part).getSize() < ((Microblock) other).getSize();
        }
        if (((Microblock) part).isTransparent() != ((Microblock) other).isTransparent()) {
            return ((Microblock) part).isTransparent();
        }
        return ((Microblock) part).getShape() > ((Microblock) other).getShape();
    }

    private static final class ShrinkPosts extends AbstractFunction1<TMultiPart, BoxedUnit>
            implements scala.Serializable {

        private static final long serialVersionUID = 0L;
        private final PostMicroblockClient part;

        ShrinkPosts(PostMicroblockClient part) {
            this.part = part;
        }

        @Override
        public BoxedUnit apply(TMultiPart other) {
            if (other instanceof PostMicroblock && !other.equals(part)) {
                part.shrinkPost((PostMicroblock) other);
            }
            return BoxedUnit.UNIT;
        }
    }
}
