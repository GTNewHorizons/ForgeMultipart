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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.junit.jupiter.api.Test;

import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.multipart.examples.PartTraversalExample;
import scala.Function1;
import scala.Tuple2;
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

    private static final Vec3 ORIGIN = Vec3.createVectorHelper(0, 0, 0);

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
        assertSame(part, tile.partList().apply(0));
    }

    @Test
    void jPartListKeepsItsCapturedSequenceWhenTheTilePublishesAnother() {
        TileMultipart tile = new TileMultipart();
        CountingPart first = new CountingPart("first");
        CountingPart second = new CountingPart("second");
        tile.addPart_do(first);
        List<TMultiPart> captured = tile.jPartList();

        tile.addPart_do(second);
        assertEquals(Arrays.asList(first), captured);
        assertEquals(Arrays.asList(first, second), tile.jPartList());
        assertThrows(UnsupportedOperationException.class, () -> captured.add(second));
        tile.clearParts();
        assertEquals(Arrays.asList(first), captured);
        assertTrue(tile.jPartList().isEmpty());
    }

    @Test
    void readQueriesAndPublishedViewsAcceptAMutableSeq() {
        TileMultipart tile = new TileMultipart();
        List<TMultiPart> backing = new ArrayList<>();
        backing.add(new LightPart(4));
        tile.partList_$eq(JavaConversions.asScalaBuffer(backing));
        List<TMultiPart> publishedView = tile.jPartList();

        assertEquals(4, tile.getLightValue());
        assertFalse(tile.canPlaceTorchOnTop());

        TorchSupportingPart support = new TorchSupportingPart();
        backing.add(new LightPart(11));
        backing.add(support);

        assertEquals(3, publishedView.size(), "The Java ABI exposes a live view of the supplied Seq");
        assertSame(support, tile.partList().apply(2));
        assertEquals(11, tile.getLightValue());
        assertTrue(tile.canPlaceTorchOnTop());
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
    void overriddenPartListDrivesQueriesAndCallbacks() {
        AccessorTile tile = new AccessorTile();
        CountingPart first = new CountingPart("first");
        RayTracingPart hit = new RayTracingPart("hit", 1d);
        tile.parts = seq(first, new LightPart(13), new TorchSupportingPart(), hit);
        tile.loadFrom(new TileMultipart());

        assertEquals(4, tile.jPartList().size());
        assertSame(tile, first.tile());
        assertEquals(13, tile.getLightValue());
        assertTrue(tile.canPlaceTorchOnTop());
        assertFalse(tile.canReplacePart(first, hit));
        tile.onChunkLoad();
        assertEquals(1, first.chunkLoads);
        tile.parts = seq(new RayTracingPart("missing", null), hit);
        assertEquals(1, index(tile.collisionRayTrace(ORIGIN, ORIGIN)));
        tile.harvestPart(1, null, null);
        assertEquals(1, hit.harvests);
    }

    @Test
    void overriddenPartListReceivesMutationsAndCopies() {
        AccessorTile source = new AccessorTile();
        CountingPart first = new CountingPart("first");
        Seq<TMultiPart> before = source.parts;
        source.addPart_do(first);
        assertEquals(1, source.writes);
        assertEquals(seq(first), source.parts);
        assertTrue(before.isEmpty());

        AccessorTile target = new AccessorTile();
        target.copyFrom(source);
        assertEquals(1, target.writes);
        assertSame(source.parts, target.parts);
        target.loadFrom(source);
        assertSame(target, first.tile());
        target.clearParts();
        assertEquals(2, target.writes);
        assertTrue(target.parts.isEmpty());
        assertEquals(seq(first), source.parts);
    }

    @Test
    void overriddenPartListKeepsTheCapturedTraversalAndDetachedPartGuard() {
        AccessorTile tile = new AccessorTile();
        CountingPart first = new CountingPart("first");
        CountingPart removed = new CountingPart("removed");
        CountingPart last = new CountingPart("last");
        CountingPart added = new CountingPart("added");
        tile.parts = seq(first, removed, last);
        tile.loadFrom(new TileMultipart());
        List<String> visited = new ArrayList<>();

        tile.operate(action(part -> {
            visited.add(part.getType());
            if (part == first) {
                tile.partList_$eq(seq(first, last, added));
                removed.tile_$eq(null);
                added.bind(tile);
            }
        }));

        assertEquals(Arrays.asList("first", "last"), visited);
        assertEquals(seq(first, last, added), tile.parts);
    }

    @Test
    void overriddenPartListKeepsLightAndResistanceLookupOrder() {
        int[] reads = { 0 };
        TileMultipart tile = new TileMultipart() {

            @Override
            public Seq<TMultiPart> partList() {
                return ++reads[0] == 1 ? seq(new ResistancePart(9f)) : seq(new LightPart(13));
            }
        };
        assertEquals(13, tile.getLightValue());
        assertEquals(2, reads[0]);
        reads[0] = 0;
        assertEquals(9f, tile.getExplosionResistance(null));
        assertEquals(1, reads[0]);
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
    void replacedTilesDoNotForwardQueuedCallbacksAfterTransfer() {
        for (boolean mutable : new boolean[] { false, true }) {
            TileMultipart oldTile = new TileMultipart();
            CountingPart part = new CountingPart("transferred");
            oldTile.addPart_do(part);
            if (mutable) {
                oldTile.partList_$eq(JavaConversions.asScalaBuffer(new ArrayList<>(oldTile.jPartList())));
            }
            TileMultipart replacement = new TileMultipart();
            replacement.from(oldTile);
            assertSame(replacement, part.tile());

            oldTile.onChunkLoad();
            assertEquals(0, part.chunkLoads);
            replacement.onChunkLoad();
            assertEquals(1, part.chunkLoads);
        }
    }

    @Test
    void lifecycleCallbacksStillDispatchThroughAnOperateOverride() {
        List<String> calls = new ArrayList<>();
        TileMultipart tile = new TileMultipart() {

            @Override
            public void operate(Function1<TMultiPart, BoxedUnit> f) {
                calls.add("operate");
                super.operate(f);
            }
        };
        CountingPart part = new CountingPart("part");
        tile.addPart_do(part);

        tile.onChunkLoad();
        tile.onNeighborBlockChange();
        assertEquals(Arrays.asList("operate", "operate"), calls);
        assertEquals(1, part.chunkLoads);
    }

    @Test
    void operateChecksOwnershipAndStopsAtTheOriginalCallbackFailure() {
        TileMultipart tile = new TileMultipart();
        CountingPart detached = new CountingPart("detached");
        CountingPart rebound = new CountingPart("rebound");
        CountingPart last = new CountingPart("last");
        tile.addPart_do(detached);
        tile.addPart_do(rebound);
        tile.addPart_do(last);
        detached.bind(null);
        rebound.bind(new TileMultipart());
        List<String> visited = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("callback failed");

        assertSame(failure, assertThrows(IllegalStateException.class, () -> tile.operate(action(part -> {
            visited.add(part.getType());
            throw failure;
        }))));
        assertEquals(Arrays.asList("last"), visited);
    }

    @Test
    void javaTraversalKeepsAccessorDispatchAndCallbackMutationSemantics() {
        AccessorTile tile = new AccessorTile();
        CountingPart first = new CountingPart("first");
        CountingPart removed = new CountingPart("removed");
        CountingPart rebound = new CountingPart("rebound");
        CountingPart added = new CountingPart("added");
        tile.parts = seq(first, removed, rebound);
        tile.loadFrom(new TileMultipart());
        List<String> visited = new ArrayList<>();
        List<String> nested = new ArrayList<>();

        tile.forEachPart(part -> {
            visited.add(part.getType());
            if (part == first) {
                tile.partList_$eq(seq(first, rebound, added));
                removed.bind(null);
                rebound.bind(new TileMultipart());
                added.bind(tile);
                tile.forEachPart(p -> nested.add(p.getType()));
            }
        });

        assertEquals(Arrays.asList("first"), visited);
        assertEquals(Arrays.asList("first", "added"), nested);
    }

    @Test
    void javaTraversalUsesTheLegacyOverrideAndDoesNotReplaceItsLifecycleHook() {
        CountingPart supplied = new CountingPart("supplied");
        List<String> calls = new ArrayList<>();
        TileMultipart tile = new TileMultipart() {

            @Override
            public void operate(Function1<TMultiPart, BoxedUnit> f) {
                calls.add("operate");
                f.apply(supplied);
            }

            @Override
            public void forEachPart(Consumer<TMultiPart> consumer) {
                calls.add("forEachPart");
                super.forEachPart(consumer);
            }
        };
        List<TMultiPart> visited = new ArrayList<>();
        tile.forEachPart(visited::add);
        tile.onChunkLoad();

        assertEquals(Arrays.asList("forEachPart", "operate", "operate"), calls);
        assertEquals(Arrays.asList(supplied), visited);
        assertEquals(1, supplied.chunkLoads);
    }

    @Test
    void javaTraversalRetainsMutableSequenceAndCallbackFailureBehavior() {
        TileMultipart tile = new TileMultipart();
        tile.forEachPart(null);
        CountingPart detached = new CountingPart("detached");
        tile.partList_$eq(seq(detached));
        tile.forEachPart(null);
        detached.bind(tile);
        assertThrows(NullPointerException.class, () -> tile.forEachPart(null));

        CountingPart last = new CountingPart("last");
        last.bind(tile);
        tile.partList_$eq(JavaConversions.asScalaBuffer(new ArrayList<>(Arrays.asList(detached, last))));
        List<TMultiPart> visited = new ArrayList<>();
        tile.forEachPart(visited::add);
        assertEquals(Arrays.asList(detached, last), visited);
        visited.clear();
        IllegalStateException failure = new IllegalStateException("callback failed");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> tile.forEachPart(part -> {
            visited.add(part);
            throw failure;
        })));
        assertEquals(Arrays.asList(detached), visited);
    }

    @Test
    void javaExamplesDistinguishCollectionAccessFromBoundPartTraversal() {
        TileMultipart tile = new TileMultipart();
        CountingPart first = new CountingPart("first");
        CountingPart detached = new CountingPart("detached");
        tile.addPart_do(first);
        tile.addPart_do(detached);
        detached.bind(null);

        assertEquals(Arrays.asList("first", "detached"), PartTraversalExample.partTypes(tile));
        assertEquals(Arrays.asList("first"), PartTraversalExample.boundPartTypes(tile));
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

    @Test
    void rayTraceAllTagsEachHitWithItsPartListIndex() {
        TileMultipart tile = new TileMultipart();
        tile.addPart_do(new RayTracingPart("far", 9d));
        tile.addPart_do(new RayTracingPart("missing", null));
        tile.addPart_do(new RayTracingPart("near", 1d));

        List<ExtendedMOP> hits = hits(tile);

        // Sorted nearest first, but the part that missed still consumed its index.
        assertEquals(Arrays.asList(2, 0), Arrays.asList(index(hits.get(0)), index(hits.get(1))));
    }

    @Test
    void rayTraceAllIndicesStillSelectTheProducingPart() {
        TileMultipart tile = new TileMultipart();
        tile.addPart_do(new RayTracingPart("far", 9d));
        tile.addPart_do(new RayTracingPart("missing", null));
        tile.addPart_do(new RayTracingPart("near", 1d));

        // The round trip every click, activate, harvest and pick block depends on.
        for (ExtendedMOP hit : hits(tile)) {
            TMultiPart part = tile.partList().apply(index(hit));
            assertSame(hit, ((RayTracingPart) part).produced);
        }
    }

    @Test
    void rayTraceAllNestsThePartsOwnDataUnderTheIndex() {
        TileMultipart tile = new TileMultipart();
        RayTracingPart part = new RayTracingPart("only", 1d);
        part.data = "subhit";
        tile.addPart_do(part);

        ExtendedMOP hit = hits(tile).get(0);

        assertEquals("subhit", ExtendedMOP.<Tuple2<Object, Object>>getData(hit)._2());
        assertEquals(-1, hit.subHit, "data is assigned directly, so subHit keeps its default");
    }

    @Test
    void rayTraceAllIsEmptyWhenNothingIsHit() {
        TileMultipart tile = new TileMultipart();
        assertTrue(hits(tile).isEmpty());

        tile.addPart_do(new RayTracingPart("missing", null));
        assertTrue(hits(tile).isEmpty());
    }

    @Test
    void collisionRayTraceReturnsTheNearestHitOrNull() {
        TileMultipart tile = new TileMultipart();
        assertNull(tile.collisionRayTrace(ORIGIN, ORIGIN));

        tile.addPart_do(new RayTracingPart("far", 9d));
        RayTracingPart near = new RayTracingPart("near", 1d);
        tile.addPart_do(near);

        ExtendedMOP nearest = tile.collisionRayTrace(ORIGIN, ORIGIN);

        assertSame(near.produced, nearest);
    }

    @Test
    void harvestPartSelectsByPartListIndex() {
        TileMultipart tile = new TileMultipart();
        RayTracingPart first = new RayTracingPart("first", null);
        RayTracingPart second = new RayTracingPart("second", null);
        tile.addPart_do(first);
        tile.addPart_do(second);

        tile.harvestPart(1, null, null);

        assertEquals(0, first.harvests);
        assertEquals(1, second.harvests);
        assertThrows(IndexOutOfBoundsException.class, () -> tile.harvestPart(2, null, null));
    }

    private static List<ExtendedMOP> hits(TileMultipart tile) {
        List<ExtendedMOP> list = new ArrayList<>();
        for (ExtendedMOP hit : tile.rayTraceAll(ORIGIN, ORIGIN)) {
            list.add(hit);
        }
        return list;
    }

    private static int index(ExtendedMOP hit) {
        return (Integer) ExtendedMOP.<Tuple2<Object, Object>>getData(hit)._1();
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

    private static final class AccessorTile extends TileMultipart {

        private Seq<TMultiPart> parts = seq();
        private int writes;

        @Override
        public Seq<TMultiPart> partList() {
            return parts;
        }

        @Override
        public void partList_$eq(Seq<TMultiPart> value) {
            writes++;
            parts = value;
        }
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

    /** Returns a canned hit so raytrace bookkeeping can be checked without a world. */
    private static final class RayTracingPart extends CountingPart {

        private final Double dist;
        private Object data;
        private ExtendedMOP produced;
        private int harvests;

        private RayTracingPart(String type, Double dist) {
            super(type);
            this.dist = dist;
        }

        @Override
        public ExtendedMOP collisionRayTrace(Vec3 start, Vec3 end) {
            if (dist == null) {
                return null;
            }
            produced = new ExtendedMOP(0, 0, 0, 0, ORIGIN, data);
            produced.dist = dist;
            return produced;
        }

        @Override
        public void harvest(MovingObjectPosition hit, EntityPlayer player) {
            harvests++;
        }
    }
}
