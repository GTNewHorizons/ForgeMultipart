package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.multipart.INeighborTileChange;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.asm.MultipartMixinFactory;
import codechicken.multipart.scalatraits.TTileChangeTile;
import scala.collection.immutable.Nil$;

/** Consumer-visible state and behavior of the generated neighbor-tile-change trait. */
class TTileChangeTileFunctionalTest {

    @Test
    void theFlagStartsFalseAndIsPublishedThroughGetWeakChanges() throws Exception {
        TileMultipart tile = newTileChangeTile();

        assertFalse(weakTileChanges(tile));
        assertFalse(tile.getWeakChanges());
    }

    @Test
    void bindPartOnlyEverSetsTheFlag() {
        TileMultipart tile = newTileChangeTile();

        tile.bindPart(new PlainPart());
        assertFalse(tile.getWeakChanges(), "A part that is not an INeighborTileChange contributes nothing");

        tile.bindPart(new ChangePart("quiet", false));
        assertFalse(tile.getWeakChanges());

        tile.bindPart(new ChangePart("weak", true));
        assertTrue(tile.getWeakChanges());

        // The flag is or-ed in, so a later quiet part cannot clear it.
        tile.bindPart(new ChangePart("quiet2", false));
        assertTrue(tile.getWeakChanges());
    }

    @Test
    void copyFromTakesTheFlagOnlyFromAnotherChangeTile() {
        TileMultipart source = newTileChangeTile();
        source.bindPart(new ChangePart("weak", true));
        TileMultipart target = newTileChangeTile();

        target.copyFrom(source);
        assertTrue(target.getWeakChanges());

        // A plain source leaves the existing value alone rather than clearing it.
        target.copyFrom(new TileMultipart());
        assertTrue(target.getWeakChanges());
    }

    @Test
    void clearPartsResetsTheFlagAndTheSuperclassList() {
        TileMultipart tile = newTileChangeTile();
        tile.addPart_do(new ChangePart("weak", true));
        assertTrue(tile.getWeakChanges());

        tile.clearParts();

        assertFalse(tile.getWeakChanges());
        assertTrue(tile.jPartList().isEmpty());
    }

    @Test
    void partRemovedRecomputesTheFlagFromTheRemainingParts() {
        TileMultipart tile = newTileChangeTile();
        ChangePart weak = new ChangePart("weak", true);
        ChangePart quiet = new ChangePart("quiet", false);
        tile.addPart_do(weak);
        tile.addPart_do(quiet);
        assertTrue(tile.getWeakChanges());

        // partRemoved recomputes from the list, so the part must already be gone from it.
        tile.partList_$eq(seq(quiet));
        tile.partRemoved(weak, 0);
        assertFalse(tile.getWeakChanges());

        tile.partList_$eq(seq(quiet, weak));
        tile.partRemoved(quiet, 0);
        assertTrue(tile.getWeakChanges(), "A remaining weak part keeps the flag set");
    }

    @Test
    void neighborChangesDispatchOnlyWithinTwoBlocksAlongOneAxis() {
        TileMultipart tile = newTileChangeTile();
        ChangePart part = new ChangePart("weak", true);
        tile.addPart_do(part);

        // The tile sits at the origin, so the neighbor coordinates are the offset.
        tile.onNeighborTileChange(1, 0, 0);
        tile.onNeighborTileChange(0, -2, 0);
        assertEquals(Arrays.asList("5:false", "0:true"), part.calls);

        part.calls.clear();
        tile.onNeighborTileChange(0, 0, 0); // no distance
        tile.onNeighborTileChange(3, 0, 0); // too far
        tile.onNeighborTileChange(1, 1, 0); // not axial, so no side
        assertTrue(part.calls.isEmpty());
    }

    @Test
    void onlyPartsWantingNeighborChangesAreCalled() {
        TileMultipart tile = newTileChangeTile();
        ChangePart listening = new ChangePart("weak", true);
        tile.addPart_do(new PlainPart());
        tile.addPart_do(listening);

        tile.onNeighborTileChange(0, 1, 0);

        assertEquals(Arrays.asList("1:false"), listening.calls);
    }

    private static TileMultipart newTileChangeTile() {
        int traitId = MultipartMixinFactory.getId(TTileChangeTile.class.getName().replace('.', '/'));
        BitSet traits = new BitSet();
        traits.set(traitId);
        return (TileMultipart) MultipartMixinFactory.construct(traits, Nil$.MODULE$);
    }

    /** The accessor is generated onto the runtime interface, so it is not visible to this source set. */
    private static boolean weakTileChanges(TileMultipart tile) throws Exception {
        return (Boolean) tile.getClass().getMethod("weakTileChanges").invoke(tile);
    }

    private static scala.collection.Seq<TMultiPart> seq(TMultiPart... parts) {
        return scala.collection.JavaConversions.asScalaBuffer(new ArrayList<>(Arrays.asList(parts))).toList();
    }

    private static class PlainPart extends TMultiPart {

        @Override
        public String getType() {
            return "tilechange_test:plain";
        }
    }

    private static final class ChangePart extends PlainPart implements INeighborTileChange {

        private final String type;
        private final boolean weak;
        private final List<String> calls = new ArrayList<>();

        private ChangePart(String type, boolean weak) {
            this.type = type;
            this.weak = weak;
        }

        @Override
        public String getType() {
            return "tilechange_test:" + type;
        }

        @Override
        public boolean weakTileChanges() {
            return weak;
        }

        @Override
        public void onNeighborTileChanged(int side, boolean weak) {
            calls.add(side + ":" + weak);
        }
    }
}
