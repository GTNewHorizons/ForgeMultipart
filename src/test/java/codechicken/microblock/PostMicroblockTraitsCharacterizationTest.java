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

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.JNormalOcclusion;
import codechicken.multipart.JPartialOcclusion;
import codechicken.multipart.TMultiPart;

/** Executes frozen Scala forwarders, including the real predecessor of PostMicroblock's super accessor. */
class PostMicroblockTraitsCharacterizationTest {

    @Test
    void occlusionListsAreFreshReadOnlySnapshotsWithVirtualBoundsAndPartialDispatch() throws Exception {
        Fixture f = new Fixture();
        Cuboid6 bounds = new Cuboid6(0, 0, 0, 0.5, 1, 1);
        f.set(f.part, "selectedBounds", Cuboid6.class, bounds);
        List<Cuboid6> first = f.post.getOcclusionBoxes();
        assertEquals(Arrays.asList("boxes:self", "bounds:self"), f.events);
        assertSame(bounds, first.get(0));
        assertEquals(1, first.size());
        assertThrows(UnsupportedOperationException.class, () -> first.add(bounds));
        assertThrows(UnsupportedOperationException.class, () -> first.set(0, bounds));
        assertThrows(UnsupportedOperationException.class, () -> first.remove(0));
        List<Cuboid6> second = f.post.getOcclusionBoxes();
        assertNotSame(first, second);
        f.set(f.part, "selectedBounds", Cuboid6.class, null);
        assertNull(f.post.getOcclusionBoxes().get(0));
        assertSame(bounds, first.get(0));
        bounds.max.x = 0.75;
        assertEquals(0.75, first.get(0).max.x);

        List<Cuboid6> replacement = new ArrayList<>();
        f.set(f.part, "replacementBoxes", List.class, replacement);
        f.events.clear();
        assertSame(replacement, f.post.getPartialOcclusionBoxes());
        assertEquals(Collections.singletonList("boxes:self"), f.events);
    }

    @Test
    void torchSupportUsesTheVirtualShapeOnceWithoutMaskingIt() throws Exception {
        Fixture f = new Fixture();
        for (int shape : new int[] { Integer.MIN_VALUE, -1, 0, 1, 2, 16, Integer.MAX_VALUE }) {
            f.set(f.part, "selectedShape", int.class, shape);
            f.events.clear();
            assertEquals(shape == 0, f.post.canPlaceTorchOnTop());
            assertEquals(Collections.singletonList("shape:self"), f.events);
        }
    }

    @Test
    void postsCompareOtherShapeFirstAndBypassTypeBoundsAndSuper() throws Exception {
        Fixture f = new Fixture();
        Microblock other = f.newPost("other");
        f.set(f.part, "superFailure", boolean.class, true);
        for (int ownShape : new int[] { -1, 0, 1, 2, 16, Integer.MAX_VALUE }) {
            f.set(f.part, "selectedShape", int.class, ownShape);
            for (int otherShape : new int[] { -1, 0, 1, 2, 16, Integer.MAX_VALUE }) {
                f.set(other, "selectedShape", int.class, otherShape);
                f.events.clear();
                assertEquals(ownShape != otherShape, f.part.occlusionTest(other));
                assertEquals(Arrays.asList("shape:other", "shape:self"), f.events);
            }
        }
        f.events.clear();
        assertFalse(f.part.occlusionTest(f.part));
        assertEquals(Arrays.asList("shape:self", "shape:self"), f.events);
    }

    @Test
    void alignedFacesBypassOverlapAndSuperUsingArithmeticSlotShift() throws Exception {
        Fixture f = new Fixture();
        TMultiPart face = f.newFace();
        f.set(f.part, "superFailure", boolean.class, true);
        for (int shape = -2; shape <= 3; shape++) {
            f.set(f.part, "selectedShape", int.class, shape);
            for (int slot = -4; slot < 8; slot++) {
                f.set(face, "slot", int.class, slot);
                f.events.clear();
                boolean aligned = Math.floorDiv(slot, 2) == shape;
                assertEquals(aligned, f.part.occlusionTest(face));
                List<String> expected = new ArrayList<>(Arrays.asList("type:face", "slot:face", "shape:self"));
                if (!aligned) expected.addAll(Arrays.asList("bounds:face", "boxes:self", "bounds:self"));
                assertEquals(expected, f.events);
            }
        }
    }

    @Test
    void normalAndPartialBoxesPrecedeAndShortCircuitTheRealSuperclass() throws Exception {
        Fixture f = new Fixture();
        Candidate other = new Candidate(f.events, "test:other");
        Cuboid6 separate = new Cuboid6(2, 2, 2, 3, 3, 3);
        for (boolean partialOverlap : new boolean[] { false, true }) {
            other.normal = Collections.singletonList(partialOverlap ? separate : Cuboid6.full);
            other.partial = Collections.singletonList(partialOverlap ? Cuboid6.full : separate);
            f.events.clear();
            assertFalse(f.part.occlusionTest(other));
            assertEquals(
                    Arrays.asList("type:other", "normal:other", "partial:other", "boxes:self", "bounds:self"),
                    f.events);
        }
        other.normal = Collections.singletonList(separate);
        other.partial = Collections.emptyList();
        for (boolean result : new boolean[] { false, true }) {
            f.set(f.part, "superResult", boolean.class, result);
            f.events.clear();
            assertEquals(result, f.part.occlusionTest(other));
            assertEquals(
                    Arrays.asList(
                            "type:other",
                            "normal:other",
                            "partial:other",
                            "boxes:self",
                            "bounds:self",
                            "super:self"),
                    f.events);
        }
        f.set(f.part, "superFailure", boolean.class, true);
        assertEquals(
                "super failure",
                assertThrows(IllegalStateException.class, () -> f.part.occlusionTest(other)).getMessage());
    }

    @Test
    void malformedCandidatesFailBeforeBoundsAndSuperclassDispatch() throws Exception {
        Fixture f = new Fixture();
        assertThrows(NullPointerException.class, () -> f.part.occlusionTest(null));
        assertTrue(f.events.isEmpty());
        Candidate other = new Candidate(f.events, null);
        assertThrows(NullPointerException.class, () -> f.part.occlusionTest(other));
        assertEquals(Collections.singletonList("type:other"), f.events);
        f.events.clear();
        other.type = "mcr_face";
        assertThrows(ClassCastException.class, () -> f.part.occlusionTest(other));
        assertEquals(Collections.singletonList("type:other"), f.events);
    }

    private static final class Candidate extends TMultiPart implements JNormalOcclusion, JPartialOcclusion {

        final List<String> events;
        String type;
        Iterable<Cuboid6> normal = Collections.emptyList();
        Iterable<Cuboid6> partial = Collections.emptyList();

        Candidate(List<String> events, String type) {
            this.events = events;
            this.type = type;
        }

        @Override
        public String getType() {
            events.add("type:other");
            return type;
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

    private static final class Fixture extends ClassLoader {

        final List<String> events = new ArrayList<>();
        final Microblock part;
        final PostMicroblock post;

        Fixture() throws Exception {
            super(Microblock.class.getClassLoader());
            for (String name : new String[] { "ReferencePostMicroblockBase", "ReferenceScalaPostMicroblock",
                    "ReferencePostFace" }) {
                InputStream input = Objects
                        .requireNonNull(getClass().getResourceAsStream("/compat/" + name + ".class.b64"));
                byte[] bytes;
                try (Scanner scanner = new Scanner(input, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
                    bytes = Base64.getMimeDecoder().decode(scanner.next());
                }
                defineClass(null, bytes, 0, bytes.length);
            }
            part = newPost("self");
            post = (PostMicroblock) part;
        }

        Microblock newPost(String label) throws Exception {
            Microblock result = (Microblock) loadClass("codechicken.multipart.compat.ReferenceScalaPostMicroblock")
                    .getConstructor().newInstance();
            set(result, "label", String.class, label);
            set(result, "events", List.class, events);
            return result;
        }

        TMultiPart newFace() throws Exception {
            TMultiPart result = (TMultiPart) loadClass("codechicken.multipart.compat.ReferencePostFace")
                    .getConstructor().newInstance();
            set(result, "events", List.class, events);
            return result;
        }

        void set(Object target, String name, Class<?> type, Object value) throws Exception {
            target.getClass().getMethod(name + "_$eq", type).invoke(target, value);
        }
    }
}
