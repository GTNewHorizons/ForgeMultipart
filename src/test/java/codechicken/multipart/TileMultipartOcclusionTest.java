package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.examples.OcclusionExample;
import scala.collection.JavaConversions;
import scala.collection.Seq;

/** Pair ordering, failures and the tile-level override hook behind collection occlusion queries. */
class TileMultipartOcclusionTest {

    @Test
    void legacyQueryChecksBothDirectionsInOrderAndStopsAtEitherRejection() {
        TileMultipart tile = new TileMultipart();
        List<String> calls = new ArrayList<>();
        Part first = new Part("first", calls);
        Part second = new Part("second", calls);
        Part candidate = new Part("candidate", calls);
        assertTrue(tile.occlusionTest(seq(first, second), candidate));
        assertEquals(
                Arrays.asList("first:candidate", "candidate:first", "second:candidate", "candidate:second"),
                calls);
        assertNull(first.tile(), "Unbound parts participate in geometry queries");
        calls.clear();
        first.accepts = false;
        assertFalse(tile.occlusionTest(seq(first, second), candidate));
        assertEquals(Arrays.asList("first:candidate"), calls);
        calls.clear();
        first.accepts = true;
        candidate.accepts = false;
        assertFalse(tile.occlusionTest(seq(first, second), candidate));
        assertEquals(Arrays.asList("first:candidate", "candidate:first"), calls);
    }

    @Test
    void legacyQueryRetainsLazyNullChecksAndOriginalCallbackFailures() {
        TileMultipart tile = new TileMultipart();
        List<String> calls = new ArrayList<>();
        Part existing = new Part("existing", calls);
        Part candidate = new Part("candidate", calls);
        assertTrue(tile.occlusionTest(seq(), null));
        assertThrows(NullPointerException.class, () -> tile.occlusionTest(null, candidate));
        existing.accepts = false;
        assertFalse(tile.occlusionTest(seq(existing, null), null));
        assertEquals(Arrays.asList("existing:null"), calls);
        existing.accepts = true;
        calls.clear();
        IllegalStateException failure = new IllegalStateException("candidate callback");
        candidate.failure = failure;
        assertSame(
                failure,
                assertThrows(IllegalStateException.class, () -> tile.occlusionTest(seq(existing, null), candidate)));
        assertEquals(Arrays.asList("existing:candidate", "candidate:existing"), calls);
    }

    @Test
    void replacementChecksKeepTheLegacyTileOverrideAndExcludeTheOutgoingPart() {
        List<String> calls = new ArrayList<>();
        List<Seq<TMultiPart>> inputs = new ArrayList<>();
        TileMultipart tile = new TileMultipart() {

            @Override
            public boolean occlusionTest(Seq<TMultiPart> parts, TMultiPart candidate) {
                inputs.add(parts);
                return false;
            }
        };
        Part outgoing = new Part("outgoing", calls);
        Part retained = new Part("retained", calls);
        Part candidate = new Part("candidate", calls);
        tile.partList_$eq(seq(outgoing, retained));
        assertFalse(tile.canReplacePart(outgoing, candidate));
        assertEquals(Arrays.asList(seq(retained)), inputs);
        assertTrue(calls.isEmpty(), "The override can reject without invoking pair callbacks");
        assertEquals(Arrays.asList(outgoing, retained), tile.jPartList());
    }

    @Test
    void javaQuerySnapshotsTheSuppliedCollectionAndRetainsDuplicatesAndUnboundParts() {
        TileMultipart tile = new TileMultipart();
        List<String> calls = new ArrayList<>();
        Part stored = new Part("stored", calls);
        stored.accepts = false;
        tile.setPartList(Arrays.asList(stored));
        Part first = new Part("first", calls);
        Part second = new Part("second", calls);
        Part candidate = new Part("candidate", calls);
        List<Part> input = new ArrayList<>(Arrays.asList(first, second, first));
        first.onTest = input::clear;

        assertTrue(tile.testOcclusion(input, candidate));
        assertEquals(
                Arrays.asList(
                        "first:candidate",
                        "candidate:first",
                        "second:candidate",
                        "candidate:second",
                        "first:candidate",
                        "candidate:first"),
                calls);
        assertTrue(input.isEmpty());
        assertEquals(
                Arrays.asList(stored),
                tile.jPartList(),
                "The query uses the supplied collection, not tile storage");
        assertNull(first.tile());
        assertNull(candidate.tile());
    }

    @Test
    void javaQueryPreservesTheLegacyOverrideAndDoesNotReplaceTheInternalHook() {
        List<String> calls = new ArrayList<>();
        List<Seq<TMultiPart>> inputs = new ArrayList<>();
        TileMultipart tile = new TileMultipart() {

            @Override
            public boolean occlusionTest(Seq<TMultiPart> parts, TMultiPart candidate) {
                calls.add("legacy");
                inputs.add(parts);
                return false;
            }

            @Override
            public boolean testOcclusion(Collection<? extends TMultiPart> parts, TMultiPart candidate) {
                calls.add("java");
                return super.testOcclusion(parts, candidate);
            }
        };
        Part first = new Part("first", calls);
        Part second = new Part("second", calls);
        Part candidate = new Part("candidate", calls);
        assertFalse(tile.testOcclusion(new LinkedHashSet<>(Arrays.asList(first, second)), candidate));
        tile.setPartList(Arrays.asList(first, second));
        assertFalse(tile.canReplacePart(first, candidate));
        assertEquals(Arrays.asList("java", "legacy", "legacy"), calls);
        assertEquals(Arrays.asList(seq(first, second), seq(second)), inputs);
    }

    @Test
    void javaQueryKeepsShortCircuitingAndCallbackFailureBoundaries() {
        TileMultipart tile = new TileMultipart();
        List<String> calls = new ArrayList<>();
        Part first = new Part("first", calls);
        Part candidate = new Part("candidate", calls);
        assertTrue(tile.testOcclusion(Collections.emptyList(), null));
        assertThrows(NullPointerException.class, () -> tile.testOcclusion(null, candidate));
        first.accepts = false;
        assertFalse(tile.testOcclusion(Arrays.asList(first, null), null));
        assertEquals(Arrays.asList("first:null"), calls);
        first.accepts = true;
        candidate.accepts = false;
        calls.clear();
        assertFalse(tile.testOcclusion(Arrays.asList(first, null), candidate));
        assertEquals(Arrays.asList("first:candidate", "candidate:first"), calls);
        calls.clear();
        IllegalStateException failure = new IllegalStateException("candidate callback");
        candidate.failure = failure;
        assertSame(
                failure,
                assertThrows(
                        IllegalStateException.class,
                        () -> tile.testOcclusion(Arrays.asList(first, null), candidate)));
        assertEquals(Arrays.asList("first:candidate", "candidate:first"), calls);
    }

    @Test
    void javaShapeExampleDistinguishesTouchingFromOverlappingBounds() {
        TileMultipart tile = new TileMultipart();
        NormallyOccludedPart existing = new NormallyOccludedPart(new Cuboid6(0, 0, 0, 0.5, 1, 1));
        List<NormallyOccludedPart> input = Arrays.asList(existing);
        assertTrue(OcclusionExample.fitsShape(tile, input, new Cuboid6(0.5, 0, 0, 1, 1, 1)));
        assertFalse(OcclusionExample.fitsShape(tile, input, new Cuboid6(0.25, 0, 0, 1, 1, 1)));
        assertTrue(tile.jPartList().isEmpty());
        assertNull(existing.tile());
    }

    private static Seq<TMultiPart> seq(TMultiPart... parts) {
        return JavaConversions.asScalaBuffer(Arrays.asList(parts)).toList();
    }

    private static class Part extends TMultiPart {

        private final String name;
        private final List<String> calls;
        private boolean accepts = true;
        private RuntimeException failure;
        private Runnable onTest;

        private Part(String name, List<String> calls) {
            this.name = name;
            this.calls = calls;
        }

        @Override
        public String getType() {
            return name;
        }

        @Override
        public boolean occlusionTest(TMultiPart other) {
            calls.add(name + ":" + (other == null ? "null" : other.getType()));
            if (onTest != null) {
                onTest.run();
            }
            if (failure != null) {
                throw failure;
            }
            return accepts;
        }
    }
}
