package codechicken.microblock;

import static codechicken.multipart.PartMap.edgeAxisMask;
import static codechicken.multipart.PartMap.unpackEdgeBits;

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.MatchError;

public final class MicroOcclusion$ {

    public static final MicroOcclusion$ MODULE$ = new MicroOcclusion$();

    private MicroOcclusion$() {}

    public void shrink(Cuboid6 renderBounds, Cuboid6 bounds, int side) {
        switch (side) {
            case -1:
                return;
            case 0:
                if (renderBounds.min.y < bounds.max.y) {
                    renderBounds.min.y = bounds.max.y;
                }
                return;
            case 1:
                if (renderBounds.max.y > bounds.min.y) {
                    renderBounds.max.y = bounds.min.y;
                }
                return;
            case 2:
                if (renderBounds.min.z < bounds.max.z) {
                    renderBounds.min.z = bounds.max.z;
                }
                return;
            case 3:
                if (renderBounds.max.z > bounds.min.z) {
                    renderBounds.max.z = bounds.min.z;
                }
                return;
            case 4:
                if (renderBounds.min.x < bounds.max.x) {
                    renderBounds.min.x = bounds.max.x;
                }
                return;
            case 5:
                if (renderBounds.max.x > bounds.min.x) {
                    renderBounds.max.x = bounds.min.x;
                }
                return;
            default:
                throw new MatchError(side);
        }
    }

    public int shrinkFrom(JMicroShrinkRender part, JMicroShrinkRender other, Cuboid6 renderBounds) {
        if (shrinkTest(part, other)) {
            shrink(renderBounds, other.getBounds(), shrinkSide(part.getSlot(), other.getSlot()));
        } else if (other.getSlot() < 6 && !other.isTransparent()) {
            boolean coversFace;
            switch (other.getSlot()) {
                case 0:
                    coversFace = renderBounds.min.y <= 0;
                    break;
                case 1:
                    coversFace = renderBounds.max.y >= 1;
                    break;
                case 2:
                    coversFace = renderBounds.min.z <= 0;
                    break;
                case 3:
                    coversFace = renderBounds.max.z >= 1;
                    break;
                case 4:
                    coversFace = renderBounds.min.x <= 0;
                    break;
                case 5:
                    coversFace = renderBounds.max.x >= 1;
                    break;
                default:
                    throw new MatchError(other.getSlot());
            }
            if (coversFace) {
                return 1 << other.getSlot();
            }
        }
        return 0;
    }

    public int shrink(JMicroShrinkRender part, Cuboid6 renderBounds, int slots) {
        int renderMask = 0;
        TileMultipart tile = ((TMultiPart) part).tile();
        for (int slot = 0; slot < slots; slot++) {
            if (slot != part.getSlot()) {
                TMultiPart other = tile.partMap(slot);
                if (other instanceof JMicroShrinkRender) {
                    renderMask |= shrinkFrom(part, (JMicroShrinkRender) other, renderBounds);
                }
            }
        }
        return renderMask;
    }

    public int shrinkSide(int firstSlot, int secondSlot) {
        if (secondSlot < 6) {
            return secondSlot;
        }
        if (firstSlot < 15) {
            int firstCorner = firstSlot - 7;
            int secondCorner = secondSlot - 7;
            switch (firstCorner ^ secondCorner) {
                case 1:
                    return secondCorner & 1;
                case 2:
                    return 2 | (secondCorner & 2) >> 1;
                case 4:
                    return 4 | (secondCorner & 4) >> 2;
                default:
                    return -1;
            }
        }
        if (secondSlot < 15) {
            int firstEdge = firstSlot - 15;
            int secondCorner = secondSlot - 7;
            int edgeBits = unpackEdgeBits(firstEdge);
            if ((secondCorner & edgeAxisMask(firstEdge)) != edgeBits) {
                return -1;
            }
            return (firstEdge & 0xC) >> 1 | (secondCorner & ~edgeBits) >> (firstEdge >> 2);
        }

        int firstEdge = firstSlot - 15;
        int secondEdge = secondSlot - 15;
        int firstBits = unpackEdgeBits(firstEdge);
        int secondBits = unpackEdgeBits(secondEdge);
        if ((firstEdge & 0xC) == (secondEdge & 0xC)) {
            switch (firstBits ^ secondBits) {
                case 1:
                    return (secondBits & 1) == 0 ? 0 : 1;
                case 2:
                    return (secondBits & 2) == 0 ? 2 : 3;
                case 4:
                    return (secondBits & 4) == 0 ? 4 : 5;
                default:
                    return -1;
            }
        }

        int mask = edgeAxisMask(firstEdge) & edgeAxisMask(secondEdge);
        if ((firstBits & mask) != (secondBits & mask)) {
            return -1;
        }
        switch (firstEdge >> 2) {
            case 0:
                return (secondBits & 1) == 0 ? 0 : 1;
            case 1:
                return (secondBits & 2) == 0 ? 2 : 3;
            case 2:
                return (secondBits & 4) == 0 ? 4 : 5;
            default:
                throw new IllegalArgumentException("Switch Falloff");
        }
    }

    public int recalcBounds(JMicroShrinkRender part, Cuboid6 renderBounds) {
        if (part.getSlot() < 6) {
            return shrink(part, renderBounds, 6);
        }
        if (part.getSlot() < 15) {
            return shrink(part, renderBounds, 15);
        }
        return shrink(part, renderBounds, 27);
    }

    public int shapePriority(int slot) {
        if (slot < 6) {
            return 2;
        }
        if (slot < 15) {
            return 1;
        }
        return 0;
    }

    public boolean shrinkTest(JMicroShrinkRender first, JMicroShrinkRender second) {
        if (first.getPriorityClass() != second.getPriorityClass()) {
            return first.getPriorityClass() < second.getPriorityClass();
        }

        int firstShape = shapePriority(first.getSlot());
        int secondShape = shapePriority(second.getSlot());
        if (firstShape != secondShape) {
            return firstShape < secondShape;
        }
        if (first.getSlot() < 6) {
            if (first.isTransparent() != second.isTransparent()) {
                return first.isTransparent();
            }
            if (first.getSize() != second.getSize()) {
                return first.getSize() < second.getSize();
            }
        } else {
            if (first.getSize() != second.getSize()) {
                return first.getSize() < second.getSize();
            }
            if (first.isTransparent() != second.isTransparent()) {
                return first.isTransparent();
            }
        }
        return first.getSlot() < second.getSlot();
    }
}
