package codechicken.multipart.scalatraits;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.INeighborTileChange;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.collection.Iterator;
import scala.collection.Seq;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

/** Mixin implementation for INeighborTileChange, reducing unnecessary computation. */
public class TTileChangeTile extends TileMultipart {

    public boolean weakTileChanges = false;

    @Override
    public void copyFrom(TileMultipart that) {
        super.copyFrom(that);
        if (that instanceof TTileChangeTile) {
            weakTileChanges = ((TTileChangeTile) that).weakTileChanges;
        }
    }

    @Override
    public void bindPart(TMultiPart part) {
        super.bindPart(part);
        if (part instanceof INeighborTileChange) {
            weakTileChanges |= ((INeighborTileChange) part).weakTileChanges();
        }
    }

    @Override
    public void clearParts() {
        super.clearParts();
        weakTileChanges = false;
    }

    @Override
    public void partRemoved(TMultiPart part, int position) {
        super.partRemoved(part, position);
        weakTileChanges = false;
        Iterator<TMultiPart> iterator = TTileChangeTileAccess.partList(this).iterator();
        while (iterator.hasNext()) {
            TMultiPart p = iterator.next();
            if (p instanceof INeighborTileChange && ((INeighborTileChange) p).weakTileChanges()) {
                weakTileChanges = true;
                return;
            }
        }
    }

    @Override
    public void onNeighborTileChange(int tileX, int tileY, int tileZ) {
        super.onNeighborTileChange(tileX, tileY, tileZ);
        BlockCoord offset = TTileChangeTileAccess.offsetFrom(this, tileX, tileY, tileZ);
        int diff = offset.absSum();
        int side = offset.toSide();

        if (side < 0 || diff <= 0 || diff > 2) {
            return;
        }

        TTileChangeTileAccess.notifyParts(this, side, diff == 2);
    }

    @Override
    public boolean getWeakChanges() {
        return weakTileChanges;
    }
}

/**
 * Keeps inherited member access outside the generated trait transformer, which also rejects a trait carrying any inner
 * class, so the operate callback lives here too.
 */
final class TTileChangeTileAccess {

    private TTileChangeTileAccess() {}

    // The parameters are Object because the trait is an interface once transformed, and javac would elide a cast
    // written against its compile-time superclass.
    static Seq<TMultiPart> partList(Object tile) {
        return ((TileMultipart) tile).partList();
    }

    static BlockCoord offsetFrom(Object tile, int tileX, int tileY, int tileZ) {
        TileMultipart multipart = (TileMultipart) tile;
        return new BlockCoord(tileX, tileY, tileZ).sub(multipart.xCoord, multipart.yCoord, multipart.zCoord);
    }

    static void notifyParts(Object tile, int side, boolean weak) {
        ((TileMultipart) tile).operate(new NeighborTileChanged(side, weak));
    }

    private static final class NeighborTileChanged extends AbstractFunction1<TMultiPart, BoxedUnit> {

        private final int side;
        private final boolean weak;

        private NeighborTileChanged(int side, boolean weak) {
            this.side = side;
            this.weak = weak;
        }

        @Override
        public BoxedUnit apply(TMultiPart part) {
            if (part instanceof INeighborTileChange) {
                ((INeighborTileChange) part).onNeighborTileChanged(side, weak);
            }
            return BoxedUnit.UNIT;
        }
    }
}
