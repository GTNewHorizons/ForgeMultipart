package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import scala.collection.JavaConversions;
import scala.collection.Seq;

/** Equality and callback semantics of the pre-port TileMultipart implementation. */
class TileMultipartEqualityRegressionTest {

    @Test
    void internalPartChangeExcludesDistinctEqualPartsAndStillBroadcastsNull() {
        List<String> events = new ArrayList<>();
        TileMultipart tile = new TileMultipart();
        EqualPart first = new EqualPart("equal", "first", events);
        EqualPart second = new EqualPart("equal", "second", events);
        EqualPart other = new EqualPart("other", "other", events);
        add(tile, first, second, other);

        tile.internalPartChange(new EqualPart("equal", "external", events));
        assertEquals(Arrays.asList("other.changed:external"), events);
        events.clear();
        tile.internalPartChange(null);
        assertEquals(Arrays.asList("first.changed:null", "second.changed:null", "other.changed:null"), events);
    }

    @Test
    void replacementFiltersEveryEqualPartBeforeOcclusionAndDuplicateChecks() {
        List<String> events = new ArrayList<>();
        TileMultipart tile = new TileMultipart();
        EqualPart first = new EqualPart("equal", "first", events);
        EqualPart second = new EqualPart("equal", "second", events);
        EqualPart survivor = new EqualPart("other", "survivor", events);
        first.rejectOcclusion = true;
        second.rejectOcclusion = true;
        add(tile, first, second, survivor);

        assertTrue(
                tile.canReplacePart(
                        new EqualPart("equal", "external", events),
                        new EqualPart("equal", "incoming", events)));
        assertEquals(Arrays.asList("survivor.occlusion:incoming", "incoming.occlusion:survivor"), events);
        assertFalse(tile.canReplacePart(first, new EqualPart("other", "duplicate", events)));
        assertSame(first, tile.partList().apply(0));
        assertSame(second, tile.partList().apply(1));
        assertSame(survivor, tile.partList().apply(2));
    }

    @Test
    void removalFiltersAllEqualEntriesButKeepsTheOriginalIndexAndCallbackTarget() throws Throwable {
        List<String> events = new ArrayList<>();
        RecordingTile tile = new RecordingTile(events);
        EqualPart survivor = new EqualPart("other", "survivor", events);
        EqualPart first = new EqualPart("equal", "first", events);
        EqualPart second = new EqualPart("equal", "second", events);
        add(tile, survivor, first, second);
        Seq<TMultiPart> published = tile.partList();

        assertEquals(1, remove(tile, second));
        assertEquals(Arrays.asList("second.light", "second.pre", "tile.removed:second:1", "second.removed"), events);
        assertEquals(1, tile.partList().size());
        assertSame(survivor, tile.partList().head());
        assertEquals(3, published.size());
        assertNull(second.tile());
        assertSame(tile, first.tile()); // The reference only detaches the requested instance.
    }

    @Test
    void removalReadsThePublishedListAfterLightAndPreRemoveCallbacks() throws Throwable {
        List<String> events = new ArrayList<>();
        RecordingTile tile = new RecordingTile(events);
        EqualPart victim = new EqualPart("victim", "victim", events);
        EqualPart stale = new EqualPart("stale", "stale", events);
        EqualPart addedByLight = new EqualPart("light", "light", events);
        EqualPart addedByPre = new EqualPart("pre", "pre", events);
        add(tile, stale, victim);
        victim.lightAction = () -> tile.partList_$eq(seq(victim, addedByLight));
        victim.preAction = () -> {
            assertSame(addedByLight, tile.partList().apply(1));
            tile.partList_$eq(seq(addedByPre, victim, addedByLight));
        };

        assertEquals(1, remove(tile, victim));
        assertEquals(2, tile.partList().size());
        assertSame(addedByPre, tile.partList().apply(0));
        assertSame(addedByLight, tile.partList().apply(1));
        assertEquals(Arrays.asList("victim.light", "victim.pre", "tile.removed:victim:1", "victim.removed"), events);
    }

    @Test
    void removalUsesTheListPublishedByOnRemovedWhenCheckingForAnEmptyTile() throws Throwable {
        List<String> events = new ArrayList<>();
        RecordingTile tile = new RecordingTile(events);
        EqualPart victim = new EqualPart("victim", "victim", events);
        EqualPart replacement = new EqualPart("replacement", "replacement", events);
        add(tile, victim);
        victim.removedAction = () -> tile.partList_$eq(seq(replacement));

        assertEquals(0, remove(tile, victim));
        assertSame(replacement, tile.partList().head());
        assertNull(victim.tile());
    }

    @Test
    void lightFailureStopsRemovalBeforePreRemoveOrListMutation() {
        List<String> events = new ArrayList<>();
        RecordingTile tile = new RecordingTile(events);
        EqualPart victim = new EqualPart("victim", "victim", events);
        EqualPart survivor = new EqualPart("other", "survivor", events);
        add(tile, victim, survivor);
        Seq<TMultiPart> published = tile.partList();
        IllegalStateException failure = new IllegalStateException("light failed");
        victim.lightAction = () -> { throw failure; };

        assertSame(failure, assertThrows(IllegalStateException.class, () -> remove(tile, victim)));
        assertSame(published, tile.partList());
        assertSame(tile, victim.tile());
        assertEquals(Arrays.asList("victim.light"), events);
    }

    @Test
    void missingPartFailsBeforeInvokingItsLightOrRemovalHooks() {
        List<String> events = new ArrayList<>();
        RecordingTile tile = new RecordingTile(events);
        add(tile, new EqualPart("present", "present", events));
        assertThrows(IllegalArgumentException.class, () -> remove(tile, new EqualPart("absent", "absent", events)));
        assertTrue(events.isEmpty());
    }

    private static int remove(TileMultipart tile, TMultiPart part) throws Throwable {
        Method method = TileMultipart.class.getDeclaredMethod("remPart_do", TMultiPart.class, boolean.class);
        method.setAccessible(true);
        try {
            return (Integer) method.invoke(tile, part, false);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static void add(TileMultipart tile, TMultiPart... parts) {
        for (TMultiPart part : parts) tile.addPart_do(part);
    }

    private static Seq<TMultiPart> seq(TMultiPart... parts) {
        return JavaConversions.asScalaBuffer(Arrays.asList(parts)).toList();
    }

    private static final class RecordingTile extends TileMultipart {

        private final List<String> events;

        RecordingTile(List<String> events) {
            this.events = events;
        }

        @Override
        public void partRemoved(TMultiPart part, int index) {
            events.add("tile.removed:" + ((EqualPart) part).name + ":" + index);
        }
    }

    private static class EqualPart extends TMultiPart {

        private final String key;
        private final String name;
        private final List<String> events;
        private boolean rejectOcclusion;
        private Runnable lightAction = () -> {};
        private Runnable preAction = () -> {};
        private Runnable removedAction = () -> {};

        EqualPart(String key, String name, List<String> events) {
            this.key = key;
            this.name = name;
            this.events = events;
        }

        @Override
        public String getType() {
            return name;
        }

        @Override
        public boolean doesTick() {
            return false;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof EqualPart && key.equals(((EqualPart) other).key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public void onPartChanged(TMultiPart part) {
            events.add(name + ".changed:" + (part == null ? "null" : ((EqualPart) part).name));
        }

        @Override
        public boolean occlusionTest(TMultiPart other) {
            events.add(name + ".occlusion:" + ((EqualPart) other).name);
            return !rejectOcclusion;
        }

        @Override
        public int getLightValue() {
            events.add(name + ".light");
            lightAction.run();
            return 7;
        }

        @Override
        public void preRemove() {
            events.add(name + ".pre");
            preAction.run();
        }

        @Override
        public void onRemoved() {
            events.add(name + ".removed");
            removedAction.run();
        }
    }
}
