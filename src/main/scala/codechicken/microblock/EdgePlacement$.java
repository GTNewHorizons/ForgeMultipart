package codechicken.microblock;

import java.util.Objects;

import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.multipart.PartMap;
import codechicken.multipart.TMultiPart;
import scala.Tuple2;

public final class EdgePlacement$ extends PlacementProperties {

    public static final EdgePlacement$ MODULE$ = new EdgePlacement$();

    private EdgePlacement$() {}

    @Override
    public EdgeMicroClass$ microClass() {
        return EdgeMicroClass$.MODULE$;
    }

    @Override
    public EdgePlacementGrid$ placementGrid() {
        return EdgePlacementGrid$.MODULE$;
    }

    @Override
    public int opposite(int slot, int side) {
        if (slot < 0) return slot;
        int edge = slot - 15;
        return 15 + PartMap.packEdgeBits(edge, PartMap.unpackEdgeBits(edge) ^ (1 << (side >> 1)));
    }

    @Override
    public ExecutablePlacement customPlacement(MicroblockPlacement placement) {
        if ((placement.size() & 1) != 0) return null;

        Microblock part = PostMicroClass.create(placement.world().isRemote, placement.material());
        part.setShape(placement.size(), placement.hit().sideHit >> 1);
        if (placement.doExpand()) {
            Tuple2<Object, Object> data = ExtendedMOP.getData(placement.hit());
            TMultiPart hitPart = placement.htile().partList().apply((Integer) data._1());
            if (Objects.equals(hitPart.getType(), PostMicroClass.getName())) {
                Microblock microPart = (Microblock) hitPart;
                if (microPart.material() == placement.material() && microPart.getSize() + placement.size() < 8) {
                    part.shape_$eq((byte) ((microPart.getSize() + placement.size()) << 4 | microPart.getShape()));
                    return placement.expand(microPart, part);
                }
            }
        }

        if (placement.slot() >= 0) return null;
        if (placement.internal() && !placement.oppMod()) {
            return placement.internalPlacement(placement.htile(), part);
        }
        return placement.externalPlacement(part);
    }
}
