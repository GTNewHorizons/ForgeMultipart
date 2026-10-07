package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

class TMicroOcclusionClientCharacterizationTest {

    @Test
    void frozenLifecycleCallsSuperThenVirtualRecalcWithOriginalArguments() throws Exception {
        Fixture f = new Fixture();
        f.set("runRecalc", boolean.class, false);
        assertNull(f.client.renderBounds());
        assertEquals(0, f.client.renderMask());
        f.part.onAdded();
        assertEquals(Arrays.asList("superAdded", "recalc"), f.events);
        f.events.clear();
        f.part.onPartChanged(f.part);
        assertSame(f.part, f.get("seenPart"));
        assertEquals(Arrays.asList("superChanged", "recalc"), f.events);
        f.events.clear();
        MCDataInput packet = (MCDataInput) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { MCDataInput.class },
                (proxy, method, args) -> { throw new AssertionError("Unexpected packet read: " + method.getName()); });
        f.part.read(packet);
        assertSame(packet, f.get("seenPacket"));
        assertEquals(Arrays.asList("superRead", "recalc"), f.events);
        f.events.clear();
        f.part.read(null);
        assertNull(f.get("seenPacket"));
        f.part.onPartChanged(null);
        assertNull(f.get("seenPart"));
        assertEquals(Arrays.asList("superRead", "recalc", "superChanged", "recalc"), f.events);
    }

    @Test
    void predecessorFailuresSkipRecalculationAndRecalcFailuresFollowSuper() throws Exception {
        Fixture f = new Fixture();
        RuntimeException failure = new IllegalStateException("sentinel");
        f.client.renderBounds_$eq(Cuboid6.full);
        f.client.renderMask_$eq(91);
        Runnable[] calls = { f.part::onAdded, () -> f.part.onPartChanged(null), () -> f.part.read(null) };
        String[] names = { "superAdded", "superChanged", "superRead" };
        for (int i = 0; i < calls.length; i++) {
            f.set("failure", RuntimeException.class, failure);
            f.events.clear();
            assertSame(failure, assertThrows(IllegalStateException.class, calls[i]::run));
            assertEquals(Collections.singletonList(names[i]), f.events);
            f.set("failure", RuntimeException.class, null);
            f.set("recalcFailure", RuntimeException.class, failure);
            f.events.clear();
            assertSame(failure, assertThrows(IllegalStateException.class, calls[i]::run));
            assertEquals(Arrays.asList(names[i], "recalc"), f.events);
            assertSame(Cuboid6.full, f.client.renderBounds());
            assertEquals(91, f.client.renderMask());
        }
    }

    @Test
    void lifecycleRecalculatesUsingStatePublishedByItsPredecessor() throws Exception {
        Fixture f = new Fixture();
        Cuboid6 changed = new Cuboid6(0.2, 0.3, 0.4, 0.8, 0.9, 1);
        f.set("afterSuper", Runnable.class, (Runnable) () -> {
            try {
                f.set("bounds", Cuboid6.class, changed);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
        for (Runnable call : new Runnable[] { f.part::onAdded, () -> f.part.onPartChanged(null),
                () -> f.part.read(null) }) {
            f.set("bounds", Cuboid6.class, null);
            f.client.renderMask_$eq(63);
            call.run();
            assertNotSame(changed, f.client.renderBounds());
            assertBounds(changed, f.client.renderBounds());
            assertEquals(0, f.client.renderMask());
        }
    }

    @Test
    void recalculationPublishesVirtualCopyBeforeTraversalAndReplacesOldMask() throws Exception {
        Fixture f = new Fixture();
        Cuboid6 copy = new Cuboid6(0.1, 0.2, 0.3, 0.7, 0.8, 0.9);
        f.set("bounds", Cuboid6.class, new Cuboid6(Cuboid6.full) {

            @Override
            public Cuboid6 copy() {
                f.events.add("copy");
                return copy;
            }
        });
        f.client.renderMask_$eq(45);
        f.tile.onVisit = () -> {
            assertSame(copy, f.client.renderBounds());
            assertEquals(45, f.client.renderMask());
        };
        f.client.recalcBounds();
        assertEquals(Arrays.asList("recalc", "bounds", "copy"), f.events);
        assertEquals(Arrays.asList(0, 1, 2, 3, 5), f.tile.visited);
        assertSame(copy, f.client.renderBounds());
        assertEquals(0, f.client.renderMask());
    }

    @Test
    void priorityControlsClippingAndOpaqueMaskAndEveryRecalcStartsFromPhysicalBounds() throws Exception {
        Fixture f = new Fixture();
        Fixture neighbor = new Fixture();
        neighbor.set("slot", int.class, 0);
        neighbor.set("size", int.class, 1);
        neighbor.set("bounds", Cuboid6.class, new Cuboid6(0, 0, 0, 1, 0.125, 1));
        f.tile.parts[0] = neighbor.part;
        f.client.recalcBounds();
        assertEquals(1, f.client.renderMask());
        assertEquals(0, f.client.renderBounds().min.y);
        neighbor.set("size", int.class, 4);
        neighbor.set("bounds", Cuboid6.class, new Cuboid6(0, 0, 0, 1, 0.5, 1));
        Cuboid6 previous = f.client.renderBounds();
        f.client.recalcBounds();
        assertNotSame(previous, f.client.renderBounds());
        assertEquals(0.5, f.client.renderBounds().min.y);
        assertEquals(0, f.client.renderMask());
        assertEquals(0, ((Cuboid6) f.get("bounds")).min.y);
        f.tile.parts[0] = null;
        f.client.recalcBounds();
        assertEquals(0, f.client.renderBounds().min.y);
        assertEquals(0, f.client.renderMask());
    }

    @Test
    void copyFailuresKeepOldStateAndTraversalFailureKeepsPublishedPartialBoundsAndOldMask() throws Exception {
        Fixture f = new Fixture();
        Cuboid6 old = Cuboid6.full.copy();
        f.client.renderBounds_$eq(old);
        f.client.renderMask_$eq(37);
        f.set("bounds", Cuboid6.class, null);
        assertThrows(NullPointerException.class, f.client::recalcBounds);
        assertSame(old, f.client.renderBounds());
        RuntimeException failure = new IllegalStateException("copy");
        f.set("bounds", Cuboid6.class, new Cuboid6(Cuboid6.full) {

            @Override
            public Cuboid6 copy() {
                throw failure;
            }
        });
        assertSame(failure, assertThrows(IllegalStateException.class, f.client::recalcBounds));
        assertSame(old, f.client.renderBounds());
        assertEquals(37, f.client.renderMask());
        f.set("bounds", Cuboid6.class, old);
        Fixture neighbor = new Fixture();
        neighbor.set("slot", int.class, 0);
        neighbor.set("size", int.class, 4);
        neighbor.set("bounds", Cuboid6.class, new Cuboid6(0, 0, 0, 1, 0.5, 1));
        f.tile.parts[0] = neighbor.part;
        f.tile.onVisit = () -> { if (f.tile.visited.size() == 2) throw failure; };
        assertSame(failure, assertThrows(IllegalStateException.class, f.client::recalcBounds));
        assertNotSame(old, f.client.renderBounds());
        assertEquals(0.5, f.client.renderBounds().min.y);
        assertEquals(0, old.min.y);
        assertEquals(37, f.client.renderMask());
    }

    private static void assertBounds(Cuboid6 expected, Cuboid6 actual) {
        assertArrayEquals(
                new double[] { expected.min.x, expected.min.y, expected.min.z, expected.max.x, expected.max.y,
                        expected.max.z },
                new double[] { actual.min.x, actual.min.y, actual.min.z, actual.max.x, actual.max.y, actual.max.z },
                1e-12);
    }

    private static final class SlotTile extends TileMultipart {

        final TMultiPart[] parts = new TMultiPart[27];
        final List<Integer> visited = new ArrayList<>();
        Runnable onVisit;

        @Override
        public TMultiPart partMap(int slot) {
            visited.add(slot);
            if (onVisit != null) onVisit.run();
            return parts[slot];
        }
    }

    private static final class Fixture extends ClassLoader {

        final TMultiPart part;
        final TMicroOcclusionClient client;
        final List<String> events = new ArrayList<>();
        final SlotTile tile = new SlotTile();

        Fixture() throws Exception {
            super(TMultiPart.class.getClassLoader());
            for (String name : new String[] { "ReferenceMicroOcclusionClientBase",
                    "ReferenceScalaTMicroOcclusionClient" }) {
                InputStream input = Objects
                        .requireNonNull(getClass().getResourceAsStream("/compat/" + name + ".class.b64"));
                byte[] bytes;
                try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
                    bytes = Base64.getMimeDecoder().decode(scanner.next());
                }
                defineClass(null, bytes, 0, bytes.length);
            }
            part = (TMultiPart) loadClass("codechicken.multipart.compat.ReferenceScalaTMicroOcclusionClient")
                    .getConstructor().newInstance();
            client = (TMicroOcclusionClient) part;
            set("events", List.class, events);
            part.bind(tile);
        }

        void set(String name, Class<?> type, Object value) throws Exception {
            part.getClass().getMethod(name + "_$eq", type).invoke(part, value);
        }

        Object get(String name) throws Exception {
            return part.getClass().getMethod(name).invoke(part);
        }
    }
}
