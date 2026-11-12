package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.JNormalOcclusion;
import codechicken.multipart.JPartialOcclusion;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

class HollowMicroblockTraitsCharacterizationTest {

    @Test
    void centerLookupRepeatsTheVirtualTileGetterAndUsesTheVirtualSlot() throws Exception {
        Fixture f = new Fixture();
        assertEquals(8, f.hollow.getHollowSize());
        assertEquals(Arrays.asList("size", "tile"), f.events);
        ProbeTile unused = new ProbeTile(f.events);
        ProbeTile selected = new ProbeTile(f.events);
        f.set("firstTile", TileMultipart.class, unused);
        f.set("secondTile", TileMultipart.class, selected);
        f.set("slot", int.class, 37);
        for (TMultiPart occupant : new TMultiPart[] { null, new Candidate(f.events) }) {
            selected.center = occupant;
            f.reset();
            assertEquals(8, f.hollow.getHollowSize());
            assertEquals(Arrays.asList("size", "tile", "tile", "map:6"), f.events);
        }
        Connector connector = new Connector(f.events);
        selected.center = connector;
        for (int size : new int[] { Integer.MIN_VALUE, -1, 0, 8, 11, 32, Integer.MAX_VALUE }) {
            connector.size = size;
            f.reset();
            assertEquals(size, f.hollow.getHollowSize());
            assertEquals(Arrays.asList("size", "tile", "tile", "map:6", "slot", "connector:37"), f.events);
        }
        f.set("secondTile", TileMultipart.class, null);
        f.reset();
        assertThrows(NullPointerException.class, f.hollow::getHollowSize);
        assertEquals(Arrays.asList("size", "tile", "tile"), f.events);
    }

    @Test
    void collisionsUseSeparateRawShapeReadsAndProduceFreshReadOnlyBoxes() throws Exception {
        Fixture f = new Fixture();
        f.set("overrideSize", boolean.class, true);
        f.set("firstShape", byte.class, (byte) 32);
        f.set("secondShape", byte.class, (byte) 113);
        List<Cuboid6> boxes = f.hollow.getCollisionBoxes();
        assertEquals(Arrays.asList("collision", "size", "shape", "shape"), f.events);
        assertEquals(4, boxes.size());
        for (Cuboid6 box : boxes) {
            assertEquals(0.75, box.min.y, 1e-12);
            assertEquals(1, box.max.y, 1e-12);
        }
        assertEquals(0.1875, volume(boxes), 1e-12);
        assertThrows(UnsupportedOperationException.class, () -> boxes.add(Cuboid6.full));
        assertThrows(UnsupportedOperationException.class, () -> boxes.set(0, Cuboid6.full));
        assertThrows(UnsupportedOperationException.class, () -> boxes.remove(0));
        f.reset();
        List<Cuboid6> again = f.hollow.getCollisionBoxes();
        assertNotSame(boxes, again);
        for (int i = 0; i < 4; i++) assertNotSame(boxes.get(i), again.get(i));
        boxes.get(0).min.x = 123;
        assertNotEquals(123, again.get(0).min.x);
    }

    @Test
    void collisionGeometryDoesNotClampOpeningOrSignedThickness() throws Exception {
        Fixture f = new Fixture();
        f.set("overrideSize", boolean.class, true);
        f.set("firstShape", byte.class, (byte) 0x80);
        f.set("secondShape", byte.class, (byte) 0);
        for (int size : new int[] { -8, 0, 8, 24 }) {
            f.set("hollowSize", int.class, size);
            f.reset();
            List<Cuboid6> boxes = f.hollow.getCollisionBoxes();
            double end = 0.5 - size / 32D;
            assertBox(new Cuboid6(0, -1, Math.min(0, end), 1, 0, Math.max(0, end)), boxes.get(0));
        }
        for (int side = 6; side < 16; side++) {
            f.set("secondShape", byte.class, (byte) side);
            f.reset();
            assertThrows(ArrayIndexOutOfBoundsException.class, f.hollow::getCollisionBoxes);
            assertEquals(Arrays.asList("collision", "size", "shape", "shape"), f.events);
        }
    }

    @Test
    void subpartsDispatchToCollisionAndCopyOrderedBoundsIntoAMutableList() throws Exception {
        Fixture f = new Fixture();
        List<Cuboid6> input = new ArrayList<>(Arrays.asList(new Cuboid6(0, 1, 2, 3, 4, 5), Cuboid6.full.copy()));
        f.set("overrideCollision", boolean.class, true);
        f.set("collision", List.class, input);
        List<IndexedCuboid6> result = f.hollow.getSubParts();
        assertEquals(Collections.singletonList("collision"), f.events);
        assertEquals(2, result.size());
        for (int i = 0; i < 2; i++) {
            assertEquals(0, result.get(i).data);
            assertBox(input.get(i), result.get(i));
            assertNotSame(input.get(i), result.get(i));
            assertNotSame(input.get(i).min, result.get(i).min);
        }
        input.get(0).min.x = 99;
        input.clear();
        assertEquals(0, result.get(0).min.x);
        result.add(new IndexedCuboid6(1, Cuboid6.full));
        result.set(0, result.get(1));
        result.remove(0);
        assertEquals(2, result.size());
        f.set("collision", List.class, Collections.singletonList(null));
        assertThrows(NullPointerException.class, f.hollow::getSubParts);
        f.set("collision", List.class, null);
        assertThrows(NullPointerException.class, f.hollow::getSubParts);
    }

    @Test
    void normalAndPartialOverlapShortCircuitTheRealScalaSuperChain() throws Exception {
        Fixture f = new Fixture();
        Candidate candidate = new Candidate(f.events);
        Cuboid6 separate = new Cuboid6(2, 2, 2, 3, 3, 3);
        for (boolean partial : new boolean[] { false, true }) {
            candidate.normal = Collections.singletonList(partial ? separate : Cuboid6.full);
            candidate.partial = Collections.singletonList(partial ? Cuboid6.full : separate);
            f.reset();
            assertFalse(f.part.occlusionTest(candidate));
            assertEquals(Arrays.asList("normal:other", "partial:other", "normal:self"), f.events);
        }
        candidate.normal = Collections.singletonList(separate);
        candidate.partial = Collections.emptyList();
        for (boolean result : new boolean[] { false, true }) {
            f.set("superResult", boolean.class, result);
            f.reset();
            assertEquals(result, f.part.occlusionTest(candidate));
            assertEquals(Arrays.asList("normal:other", "partial:other", "normal:self", "super"), f.events);
        }
        f.set("superFailure", boolean.class, true);
        assertEquals(
                "super failure",
                assertThrows(IllegalStateException.class, () -> f.part.occlusionTest(candidate)).getMessage());
    }

    private static double volume(List<Cuboid6> boxes) {
        double volume = 0;
        for (Cuboid6 c : boxes) volume += (c.max.x - c.min.x) * (c.max.y - c.min.y) * (c.max.z - c.min.z);
        return volume;
    }

    private static void assertBox(Cuboid6 expected, Cuboid6 actual) {
        assertArrayEquals(
                new double[] { expected.min.x, expected.min.y, expected.min.z, expected.max.x, expected.max.y,
                        expected.max.z },
                new double[] { actual.min.x, actual.min.y, actual.min.z, actual.max.x, actual.max.y, actual.max.z },
                1e-12);
    }

    private static final class ProbeTile extends TileMultipart {

        final List<String> events;
        TMultiPart center;

        ProbeTile(List<String> events) {
            this.events = events;
        }

        @Override
        public TMultiPart partMap(int slot) {
            events.add("map:" + slot);
            return center;
        }
    }

    private static class Candidate extends TMultiPart implements JNormalOcclusion, JPartialOcclusion {

        final List<String> events;
        Iterable<Cuboid6> normal = Collections.emptyList();
        Iterable<Cuboid6> partial = Collections.emptyList();

        Candidate(List<String> events) {
            this.events = events;
        }

        @Override
        public String getType() {
            throw new AssertionError("unexpected type lookup");
        }

        @Override
        public Iterable<Cuboid6> getOcclusionBoxes() {
            events.add("normal:other");
            return normal;
        }

        @Override
        public Iterable<Cuboid6> getPartialOcclusionBoxes() {
            events.add("partial:other");
            return partial;
        }
    }

    private static final class Connector extends Candidate implements ISidedHollowConnect {

        int size;

        Connector(List<String> events) {
            super(events);
        }

        @Override
        public int getHollowSize(int side) {
            events.add("connector:" + side);
            return size;
        }
    }

    private static final class Fixture extends ClassLoader {

        final List<String> events = new ArrayList<>();
        final Microblock part;
        final HollowMicroblock hollow;

        Fixture() throws Exception {
            super(Microblock.class.getClassLoader());
            for (String name : new String[] { "ReferenceHollowMicroblockBase", "ReferenceScalaHollowMicroblock" }) {
                InputStream input = Objects
                        .requireNonNull(getClass().getResourceAsStream("/compat/" + name + ".class.b64"));
                byte[] bytes;
                try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
                    bytes = Base64.getMimeDecoder().decode(scanner.next());
                }
                defineClass(null, bytes, 0, bytes.length);
            }
            part = (Microblock) loadClass("codechicken.multipart.compat.ReferenceScalaHollowMicroblock")
                    .getConstructor().newInstance();
            hollow = (HollowMicroblock) part;
            set("events", List.class, events);
        }

        void set(String name, Class<?> type, Object value) throws Exception {
            part.getClass().getMethod(name + "_$eq", type).invoke(part, value);
        }

        void reset() throws Exception {
            events.clear();
            set("tileReads", int.class, 0);
            set("shapeReads", int.class, 0);
        }
    }
}
