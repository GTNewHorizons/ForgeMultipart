package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.entity.Entity;

import org.junit.jupiter.api.Test;

import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

/**
 * Covers the part of TileMultipart that does not need a world: list storage, the operate guard, and the pure predicates
 * built on top of them. Anything touching worldObj, the registry or packets is exercised by the Forge server suite
 * instead, because it cannot run headless.
 */
class TileMultipartCharacterizationTest {

    @Test
    void aFreshTileHoldsNoPartsAndDoesNotTick() {
        TileMultipart tile = new TileMultipart();

        assertEquals(0, tile.partList().size());
        assertTrue(tile.jPartList().isEmpty());
        assertFalse(tile.canUpdate());
        assertNull(tile.partMap(0));
    }

    @Test
    void addingPartsAppendsInOrderAndBindsThem() {
        TileMultipart tile = new TileMultipart();
        CountingPart first = new CountingPart("a");
        CountingPart second = new CountingPart("b");

        tile.addPart_do(first);
        tile.addPart_do(second);

        assertEquals(Arrays.asList(first, second), tile.jPartList());
        assertSame(tile, first.tile());
        assertSame(tile, second.tile());
    }

    @Test
    void thePartListIsReplacedRatherThanMutated() {
        TileMultipart tile = new TileMultipart();
        Seq<TMultiPart> before = tile.partList();

        tile.addPart_do(new CountingPart("a"));

        assertNotSame(before, tile.partList(), "Each mutation must publish a new immutable Seq");
        assertEquals(0, before.size(), "The previously published Seq must not change under a caller");
        assertEquals(1, tile.partList().size());
    }

    @Test
    void jPartListReflectsTheCurrentParts() {
        TileMultipart tile = new TileMultipart();
        CountingPart part = new CountingPart("a");
        tile.addPart_do(part);

        assertEquals(1, tile.jPartList().size());
        assertSame(part, tile.jPartList().get(0));
    }

    @Test
    void clearPartsEmptiesTheList() {
        TileMultipart tile = new TileMultipart();
        tile.addPart_do(new CountingPart("a"));

        tile.clearParts();

        assertEquals(0, tile.partList().size());
        assertTrue(tile.jPartList().isEmpty());
    }

    @Test
    void operateSkipsPartsWhoseTileHasBeenCleared() {
        TileMultipart tile = new TileMultipart();
        CountingPart kept = new CountingPart("a");
        CountingPart detached = new CountingPart("b");
        tile.addPart_do(kept);
        tile.addPart_do(detached);
        detached.tile_$eq(null);

        tile.onChunkLoad();

        assertEquals(1, kept.chunkLoads);
        assertEquals(0, detached.chunkLoads, "A part with a null tile must be skipped");
    }

    @Test
    void operateDoesNotVisitPartsAddedByItsCallback() {
        TileMultipart tile = new TileMultipart();
        CountingPart first = new CountingPart("first");
        CountingPart second = new CountingPart("second");
        CountingPart added = new CountingPart("added");
        List<String> visited = new ArrayList<>();
        tile.addPart_do(first);
        tile.addPart_do(second);

        tile.operate(action(part -> {
            visited.add(part.getType());
            if (part == first) {
                tile.addPart_do(added);
            }
        }));

        assertEquals(Arrays.asList("first", "second"), visited);
        assertEquals(Arrays.asList(first, second, added), tile.jPartList());
    }

    @Test
    void operateSkipsAPartDetachedByAnEarlierCallback() {
        TileMultipart tile = new TileMultipart();
        CountingPart first = new CountingPart("first");
        CountingPart detached = new CountingPart("detached");
        CountingPart last = new CountingPart("last");
        List<String> visited = new ArrayList<>();
        tile.addPart_do(first);
        tile.addPart_do(detached);
        tile.addPart_do(last);

        tile.operate(action(part -> {
            visited.add(part.getType());
            if (part == first) {
                tile.partList_$eq(seq(first, last));
                detached.tile_$eq(null);
            }
        }));

        assertEquals(Arrays.asList("first", "last"), visited);
        assertEquals(Arrays.asList(first, last), tile.jPartList());
    }

    @Test
    void operateAcceptsAMutableSeqThroughThePublishedSetter() {
        TileMultipart tile = new TileMultipart();
        CountingPart first = new CountingPart("first");
        CountingPart second = new CountingPart("second");
        tile.addPart_do(first);
        tile.addPart_do(second);
        tile.partList_$eq(JavaConversions.asScalaBuffer(new ArrayList<>(tile.jPartList())));

        tile.onChunkLoad();

        assertEquals(1, first.chunkLoads);
        assertEquals(1, second.chunkLoads);
    }

    @Test
    void ticksOnlyOnceATickingPartIsPresent() {
        TileMultipart quiet = new TileMultipart();
        assertFalse(quiet.canUpdate());

        // TMultiPart.doesTick defaults to true, so a part must opt out to leave the tile idle.
        quiet.addPart_do(new StillPart());
        assertFalse(quiet.canUpdate());

        TileMultipart ticking = new TileMultipart();
        ticking.addPart_do(new CountingPart("a"));
        assertTrue(ticking.canUpdate(), "A default part ticks, so it starts the tile ticking");
    }

    @Test
    void lightValueIsTheMaximumAndZeroWhenEmpty() {
        TileMultipart tile = new TileMultipart();
        assertEquals(0, tile.getLightValue());

        tile.addPart_do(new LightPart(4));
        tile.addPart_do(new LightPart(11));
        tile.addPart_do(new LightPart(7));

        assertEquals(11, tile.getLightValue());
    }

    @Test
    void explosionResistanceIsTheMaximumButThrowsWhenEmpty() {
        TileMultipart tile = new TileMultipart();

        assertThrows(UnsupportedOperationException.class, () -> tile.getExplosionResistance(null));

        tile.addPart_do(new ResistancePart(2f));
        tile.addPart_do(new ResistancePart(9f));

        assertEquals(9f, tile.getExplosionResistance(null));
    }

    @Test
    void occlusionTestRequiresAgreementInBothDirections() {
        TileMultipart tile = new TileMultipart();
        TMultiPart accepting = new CountingPart("a");
        RejectingPart rejecting = new RejectingPart();

        assertTrue(tile.occlusionTest(seq(accepting), new CountingPart("b")));
        assertFalse(tile.occlusionTest(seq(rejecting), new CountingPart("b")), "An existing part may refuse");
        assertFalse(tile.occlusionTest(seq(accepting), rejecting), "The incoming part may refuse");
        assertTrue(tile.occlusionTest(seq(), rejecting), "Nothing to conflict with");
    }

    @Test
    void canReplacePartIgnoresTheOutgoingPartAndRejectsDuplicates() {
        TileMultipart tile = new TileMultipart();
        RejectingPart outgoing = new RejectingPart();
        CountingPart other = new CountingPart("b");
        tile.addPart_do(outgoing);
        tile.addPart_do(other);

        // The outgoing part is excluded from the test even though it refuses everything.
        assertTrue(tile.canReplacePart(outgoing, new CountingPart("c")));
        // A part already present, other than the outgoing one, cannot be added again.
        assertFalse(tile.canReplacePart(outgoing, other));
        assertEquals(Arrays.asList(outgoing, other), tile.jPartList(), "A replacement check must not mutate order");
        assertSame(tile, outgoing.tile());
        assertSame(tile, other.tile());
    }

    @Test
    void solidityAndTorchPlacementFallBackToTheParts() {
        TileMultipart tile = new TileMultipart();

        // partMap is null on a bare tile, so nothing is solid and no torch may be placed.
        assertFalse(tile.isSolid(1));
        assertFalse(tile.canPlaceTorchOnTop());

        tile.addPart_do(new TorchSupportingPart());
        assertTrue(tile.canPlaceTorchOnTop());
    }

    private static Seq<TMultiPart> seq(TMultiPart... parts) {
        List<TMultiPart> list = new ArrayList<>(Arrays.asList(parts));
        return JavaConversions.asScalaBuffer(list).toList();
    }

    private static AbstractFunction1<TMultiPart, BoxedUnit> action(Consumer<TMultiPart> action) {
        return new AbstractFunction1<TMultiPart, BoxedUnit>() {

            @Override
            public BoxedUnit apply(TMultiPart part) {
                action.accept(part);
                return BoxedUnit.UNIT;
            }
        };
    }

    private static class CountingPart extends TMultiPart {

        private final String type;
        private int chunkLoads;

        private CountingPart(String type) {
            this.type = type;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public void onChunkLoad() {
            chunkLoads++;
        }
    }

    private static final class StillPart extends CountingPart {

        private StillPart() {
            super("still");
        }

        @Override
        public boolean doesTick() {
            return false;
        }
    }

    private static final class LightPart extends CountingPart {

        private final int light;

        private LightPart(int light) {
            super("light");
            this.light = light;
        }

        @Override
        public int getLightValue() {
            return light;
        }
    }

    private static final class ResistancePart extends CountingPart {

        private final float resistance;

        private ResistancePart(float resistance) {
            super("resistance");
            this.resistance = resistance;
        }

        @Override
        public float explosionResistance(Entity entity) {
            return resistance;
        }
    }

    private static final class RejectingPart extends CountingPart {

        private RejectingPart() {
            super("rejecting");
        }

        @Override
        public boolean occlusionTest(TMultiPart npart) {
            return false;
        }
    }

    private static final class TorchSupportingPart extends CountingPart {

        private TorchSupportingPart() {
            super("torchsupport");
        }

        @Override
        public boolean canPlaceTorchOnTop() {
            return true;
        }
    }
}
