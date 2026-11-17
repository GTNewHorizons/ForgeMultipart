package codechicken.microblock;

import codechicken.multipart.PartMap;
import codechicken.multipart.TMultiPart;

/** Occlusion rules after the retained Scala super call. */
final class TMicroOcclusionLogic {

    private TMicroOcclusionLogic() {}

    static boolean occlusionTest(TMicroOcclusion part, TMultiPart next) {
        if (!(next instanceof TMicroOcclusion)) {
            return true;
        }
        TMicroOcclusion other = (TMicroOcclusion) next;
        int shape1 = MicroOcclusion$.MODULE$.shapePriority(part.getSlot());
        int shape2 = MicroOcclusion$.MODULE$.shapePriority(other.getSlot());
        if (other.getSize() + part.getSize() > 8) {
            if (shape1 == 2 && shape2 == 2) {
                if (other.getSlot() == (part.getSlot() ^ 1)) {
                    return false;
                }
            }
            if (other.getMaterial() != part.getMaterial()) {
                if (shape1 == 1 && shape2 == 1) {
                    int axisMask = (part.getSlot() - 7) ^ (other.getSlot() - 7);
                    if (axisMask == 3 || axisMask == 5 || axisMask == 6) {
                        return false;
                    }
                }
                if (shape1 == 0 && shape2 == 1) {
                    if (!part.edgeCornerOcclusionTest(part, other)) {
                        return false;
                    }
                }
                if (shape1 == 1 && shape2 == 0) {
                    if (!part.edgeCornerOcclusionTest(other, part)) {
                        return false;
                    }
                }
                if (shape1 == 0 && shape2 == 0) {
                    int edge1 = part.getSlot() - 15;
                    int edge2 = other.getSlot() - 15;
                    if ((edge1 & 0xc) == (edge2 & 0xc) && ((edge1 & 3) ^ (edge2 & 3)) == 3) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static boolean edgeCornerOcclusionTest(TMicroOcclusion edge, TMicroOcclusion corner) {
        return ((corner.getSlot() - 7) & PartMap.edgeAxisMask(edge.getSlot() - 15))
                == PartMap.unpackEdgeBits(edge.getSlot() - 15);
    }
}
