package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.multipart.examples.PartLoadingExample;
import scala.collection.JavaConversions;
import scala.collection.Seq;

/** Storage assignment and reconstruction contracts used by GuideNH and Schematica. */
class TileMultipartLoadingTest {

    @Test
    void legacySetterSharesStorageWithoutBindingOrChangingTickState() {
        TileMultipart tile = new TileMultipart();
        Part part = new Part("part", new ArrayList<>());
        List<TMultiPart> input = new ArrayList<>(Arrays.asList(part));
        Seq<TMultiPart> sequence = JavaConversions.asScalaBuffer(input);
        tile.partList_$eq(sequence);

        assertSame(sequence, tile.partList());
        assertNull(part.tile());
        assertFalse(tile.canUpdate());
        input.clear();
        assertTrue(tile.jPartList().isEmpty());
        tile.partList_$eq(null);
        assertNull(tile.partList());
    }

    @Test
    void legacyLoadingRebuildsThroughHooksButDoesNotRemoveOldPartsOrResetTicking() {
        List<String> events = new ArrayList<>();
        LoadingTile tile = new LoadingTile(events);
        Part old = new Part("old", events);
        tile.addPart_do(old);
        events.clear();
        Part first = new Part("first", events);
        Part second = new Part("second", events);
        tile.loadParts(seq(first, second));

        assertEquals(Arrays.asList("clear", "cache:first", "bind:first", "cache:second", "bind:second"), events);
        assertEquals(Arrays.asList(first, second), tile.jPartList());
        assertSame(tile, first.tile());
        assertSame(tile, second.tile());
        assertSame(tile, old.tile());
        assertTrue(tile.canUpdate());
        events.clear();
        tile.loadParts(seq());
        assertEquals(Arrays.asList("clear"), events);
        assertTrue(tile.jPartList().isEmpty());
        assertTrue(tile.canUpdate(), "Loading does not reset the existing ticking flag");
    }

    @Test
    void legacyLoadingFailuresLeaveClearedOrPartiallyLoadedState() {
        List<String> events = new ArrayList<>();
        LoadingTile tile = new LoadingTile(events);
        Part old = new Part("old", events);
        tile.addPart_do(old);
        assertThrows(NullPointerException.class, () -> tile.loadParts((scala.collection.Iterable<TMultiPart>) null));
        assertTrue(tile.jPartList().isEmpty());
        assertSame(tile, old.tile());

        Part first = new Part("first", events);
        Part last = new Part("last", events);
        assertThrows(NullPointerException.class, () -> tile.loadParts(seq(first, null, last)));
        assertEquals(Arrays.asList(first, null), tile.jPartList());
        assertSame(tile, first.tile());
        assertNull(last.tile());
    }

    @Test
    void legacyReflectionResolvesTheSetterAndOneAssignableLoader() throws Exception {
        TileMultipart tile = new TileMultipart();
        Part part = new Part("part", new ArrayList<>());
        Seq<TMultiPart> input = seq(part);
        TileMultipart.class.getMethod("partList_$eq", Seq.class).invoke(tile, input);
        assertSame(input, tile.partList());
        assertNull(part.tile());
        Method exact = TileMultipart.class.getMethod("loadParts", scala.collection.Iterable.class);
        List<Method> matches = new ArrayList<>();
        for (Method method : TileMultipart.class.getMethods()) {
            if (method.getName().equals("loadParts") && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(input.getClass())) {
                matches.add(method);
            }
        }
        assertEquals(Arrays.asList(exact), matches);
        matches.get(0).invoke(tile, input);
        assertSame(tile, part.tile());
        assertEquals(Arrays.asList(part), tile.jPartList());
    }

    @Test
    void javaSetterCopiesInOrderAndRetainsTheLegacyOverrideAndNullState() {
        List<Seq<TMultiPart>> writes = new ArrayList<>();
        TileMultipart tile = new TileMultipart() {

            @Override
            public void partList_$eq(Seq<TMultiPart> parts) {
                writes.add(parts);
                super.partList_$eq(parts);
            }
        };
        Part part = new Part("part", new ArrayList<>());
        List<TMultiPart> input = new ArrayList<>(Arrays.asList(part, part, null));
        PartLoadingExample.stageParts(tile, input);
        assertEquals(1, writes.size());
        assertSame(writes.get(0), tile.partList());
        assertEquals(input, tile.jPartList());
        input.clear();
        assertEquals(Arrays.asList(part, part, null), tile.jPartList());
        assertThrows(UnsupportedOperationException.class, () -> tile.jPartList().clear());
        assertNull(part.tile());
        assertFalse(tile.canUpdate());
        tile.setPartList(null);
        assertNull(writes.get(1));
        assertNull(tile.partList());
        tile.setPartList(Collections.emptyList());
        assertEquals(3, writes.size());
        assertTrue(tile.jPartList().isEmpty());
    }

    @Test
    void javaLoaderUsesTheLegacyOverrideWithAnOrderedNonListCollection() {
        List<scala.collection.Iterable<TMultiPart>> calls = new ArrayList<>();
        TileMultipart tile = new TileMultipart() {

            @Override
            public void loadParts(scala.collection.Iterable<TMultiPart> parts) {
                calls.add(parts);
                super.loadParts(parts);
            }
        };
        List<String> events = new ArrayList<>();
        Part first = new Part("first", events);
        Part second = new Part("second", events);
        Collection<TMultiPart> input = new LinkedHashSet<>(Arrays.asList(first, second));
        tile.loadPartList(input);
        assertEquals(1, calls.size());
        assertEquals(Arrays.asList("bind:first", "bind:second"), events);
        input.clear();
        assertEquals(Arrays.asList(first, second), tile.jPartList());
        assertSame(tile, first.tile());
        assertSame(tile, second.tile());
        PartLoadingExample.reloadParts(tile);
        assertEquals(2, calls.size());
        assertEquals(Arrays.asList(first, second), tile.jPartList());
    }

    @Test
    void javaLoaderReadsTheLiveInputAndKeepsFailureOrdering() {
        TileMultipart tile = new TileMultipart();
        List<TMultiPart> input = new ArrayList<>();
        List<String> events = new ArrayList<>();
        Part replaced = new Part("replaced", events);
        Part replacement = new Part("replacement", events);
        Part first = new Part("first", events) {

            @Override
            public void bind(TileMultipart host) {
                super.bind(host);
                input.set(1, replacement);
            }
        };
        input.add(first);
        input.add(replaced);
        tile.loadPartList(input);
        assertEquals(Arrays.asList(first, replacement), tile.jPartList());
        assertNull(replaced.tile());
        assertSame(tile, replacement.tile());
        assertThrows(NullPointerException.class, () -> tile.loadPartList((Collection<TMultiPart>) null));
        assertTrue(tile.jPartList().isEmpty());
        assertSame(tile, first.tile());
        assertThrows(NullPointerException.class, () -> tile.loadPartList(Arrays.asList(replacement, null, replaced)));
        assertEquals(Arrays.asList(replacement, null), tile.jPartList());
        assertNull(replaced.tile());
    }

    private static Seq<TMultiPart> seq(TMultiPart... parts) {
        return JavaConversions.asScalaBuffer(Arrays.asList(parts)).toList();
    }

    private static class LoadingTile extends TileMultipart {

        private final List<String> events;

        private LoadingTile(List<String> events) {
            this.events = events;
        }

        @Override
        public void clearParts() {
            events.add("clear");
            super.clearParts();
        }

        @Override
        public void bindPart(TMultiPart part) {
            events.add("cache:" + (part == null ? "null" : part.getType()));
        }
    }

    private static class Part extends TMultiPart {

        private final String name;
        private final List<String> events;

        private Part(String name, List<String> events) {
            this.name = name;
            this.events = events;
        }

        @Override
        public String getType() {
            return name;
        }

        @Override
        public void bind(TileMultipart tile) {
            events.add("bind:" + name);
            super.bind(tile);
        }

        @Override
        public void onAdded() {
            events.add("added:" + name);
        }

        @Override
        public void onRemoved() {
            events.add("removed:" + name);
        }

        @Override
        public void onWorldJoin() {
            events.add("join:" + name);
        }
    }
}
