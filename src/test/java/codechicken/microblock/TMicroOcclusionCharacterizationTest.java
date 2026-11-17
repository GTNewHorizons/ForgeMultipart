package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import codechicken.multipart.TMultiPart;

class TMicroOcclusionCharacterizationTest {

    @Test
    void superclassRunsOnceBeforeNullAndNonMicroblockGuards() throws Exception {
        Fixture f = new Fixture();
        TMultiPart plain = new TMultiPart() {

            @Override
            public String getType() {
                throw new AssertionError("type lookup");
            }
        };
        for (TMultiPart candidate : new TMultiPart[] { null, plain, f.other }) {
            f.set(f.part, "superResult", boolean.class, false);
            f.events.clear();
            assertFalse(f.part.occlusionTest(candidate));
            assertEquals(Collections.singletonList("super:self"), f.events);
        }
        f.set(f.part, "superResult", boolean.class, true);
        for (TMultiPart candidate : new TMultiPart[] { null, plain }) {
            f.events.clear();
            assertTrue(f.part.occlusionTest(candidate));
            assertEquals(Collections.singletonList("super:self"), f.events);
        }
        f.set(f.part, "superFailure", boolean.class, true);
        f.events.clear();
        assertEquals("super", assertThrows(IllegalStateException.class, () -> f.part.occlusionTest(null)).getMessage());
        assertEquals(Collections.singletonList("super:self"), f.events);
    }

    @Test
    void sizeThresholdUsesOtherThenSelfAndKeepsSignedOverflow() throws Exception {
        Fixture f = new Fixture();
        f.configure(f.part, 0, 4, 0);
        f.configure(f.other, 1, 4, 1);
        int[][] sizes = { { 4, 4 }, { -1, 9 }, { Integer.MAX_VALUE, Integer.MAX_VALUE }, { Integer.MIN_VALUE, 0 } };
        for (int[] pair : sizes) {
            f.set(f.part, "size", int.class, pair[0]);
            f.set(f.other, "size", int.class, pair[1]);
            f.events.clear();
            assertTrue(f.part.occlusionTest(f.other));
            assertEquals(Arrays.asList("super:self", "slot:self", "slot:other", "size:other", "size:self"), f.events);
        }
        f.set(f.part, "size", int.class, Integer.MIN_VALUE);
        f.set(f.other, "size", int.class, -1);
        assertFalse(f.part.occlusionTest(f.other));
    }

    @Test
    void opposingFacesRereadOtherSlotBeforeOwnAndBypassMaterials() throws Exception {
        Fixture f = new Fixture();
        f.configure(f.part, 0, 5, 0);
        f.configure(f.other, 0, 5, 0);
        f.set(f.part, "slotAdvance", int.class, 1);
        f.set(f.part, "failOn", String.class, "material");
        f.set(f.other, "failOn", String.class, "material");
        assertFalse(f.part.occlusionTest(f.other));
        assertEquals(
                Arrays.asList(
                        "super:self",
                        "slot:self",
                        "slot:other",
                        "size:other",
                        "size:self",
                        "slot:other",
                        "slot:self"),
                f.events);
    }

    @Test
    void matchingMaterialsBypassCornerAndEdgeRulesAfterOtherFirstMaterialReads() throws Exception {
        Fixture f = new Fixture();
        for (int[] slots : new int[][] { { 7, 10 }, { 15, 18 }, { 15, 8 } }) {
            f.configure(f.part, slots[0], 5, 17);
            f.configure(f.other, slots[1], 5, 17);
            f.set(f.part, "failOn", String.class, "edge");
            f.events.clear();
            assertTrue(f.part.occlusionTest(f.other));
            assertEquals(
                    Arrays.asList(
                            "super:self",
                            "slot:self",
                            "slot:other",
                            "size:other",
                            "size:self",
                            "material:other",
                            "material:self"),
                    f.events);
        }
    }

    @Test
    void diagonalCornersAndOppositeParallelEdgesUseFreshSlotsWithoutBounds() throws Exception {
        Fixture f = new Fixture();
        for (int[] slots : new int[][] { { 7, 10 }, { 15, 18 } }) {
            f.configure(f.part, slots[0], 5, 0);
            f.configure(f.other, slots[1], 5, 1);
            f.events.clear();
            assertFalse(f.part.occlusionTest(f.other));
            assertEquals(
                    Arrays.asList(
                            "super:self",
                            "slot:self",
                            "slot:other",
                            "size:other",
                            "size:self",
                            "material:other",
                            "material:self",
                            "slot:self",
                            "slot:other"),
                    f.events);
            f.configure(f.part, slots[0], 5, 0);
            f.configure(f.other, slots[1], 5, 1);
            f.set(f.part, "slotAdvance", int.class, 1);
            assertTrue(f.part.occlusionTest(f.other));
            f.set(f.part, "slotAdvance", int.class, 0);
        }
        // Slot 6 follows the reference's corner priority; it is not rejected as an invalid face slot.
        f.configure(f.part, 6, 5, 0);
        f.configure(f.other, 7, 5, 1);
        assertTrue(f.part.occlusionTest(f.other));
        f.configure(f.part, -1, 5, 0);
        f.configure(f.other, -2, 5, 1);
        assertFalse(f.part.occlusionTest(f.other));
    }

    @Test
    void edgeCornerDelegationUsesOwnOverrideAndPassesEdgeFirstInBothDirections() throws Exception {
        Fixture f = new Fixture();
        f.set(f.part, "useActualEdge", boolean.class, false);
        f.set(f.other, "failOn", String.class, "edge");
        for (boolean ownIsEdge : new boolean[] { false, true }) {
            f.configure(f.part, ownIsEdge ? 15 : 7, 5, 0);
            f.configure(f.other, ownIsEdge ? 7 : 15, 5, 1);
            for (boolean result : new boolean[] { false, true }) {
                f.set(f.part, "edgeResult", boolean.class, result);
                f.events.clear();
                assertEquals(result, f.part.occlusionTest(f.other));
                assertEquals(
                        Arrays.asList(
                                "super:self",
                                "slot:self",
                                "slot:other",
                                "size:other",
                                "size:self",
                                "material:other",
                                "material:self",
                                "edge:self"),
                        f.events);
                assertSame(ownIsEdge ? f.part : f.other, f.part.getClass().getMethod("seenEdge").invoke(f.part));
                assertSame(ownIsEdge ? f.other : f.part, f.part.getClass().getMethod("seenCorner").invoke(f.part));
            }
        }
    }

    @Test
    void edgeCornerMappingKeepsCornerFirstAndTwoIndependentEdgeReads() throws Exception {
        Fixture f = new Fixture();
        int[] masks = { 6, 5, 3 };
        int[] positions = { 0, 2, 4, 6, 0, 4, 1, 5, 0, 1, 2, 3 };
        for (int edge = 0; edge < 12; edge++) {
            f.configure(f.part, 15 + edge, 5, 0);
            for (int corner = 0; corner < 8; corner++) {
                f.configure(f.other, 7 + corner, 5, 1);
                f.events.clear();
                assertEquals(
                        (corner & masks[edge / 4]) == positions[edge],
                        f.micro.edgeCornerOcclusionTest(f.micro, f.otherMicro));
                assertEquals(Arrays.asList("edge:self", "slot:other", "slot:self", "slot:self"), f.events);
            }
        }
        f.configure(f.part, 16, 5, 0);
        f.configure(f.other, 11, 5, 1);
        f.set(f.part, "slotAdvance", int.class, 4);
        assertTrue(f.micro.edgeCornerOcclusionTest(f.micro, f.otherMicro));
        f.configure(f.part, 15, 5, 0);
        f.set(f.part, "slotAdvance", int.class, 12);
        f.events.clear();
        assertEquals(
                "Switch Falloff",
                assertThrows(
                        IllegalArgumentException.class,
                        () -> f.micro.edgeCornerOcclusionTest(f.micro, f.otherMicro)).getMessage());
        assertEquals(Arrays.asList("edge:self", "slot:other", "slot:self", "slot:self"), f.events);
        f.configure(f.part, 27, 5, 0);
        f.events.clear();
        assertThrows(IllegalArgumentException.class, () -> f.micro.edgeCornerOcclusionTest(f.micro, f.otherMicro));
        assertEquals(Arrays.asList("edge:self", "slot:other", "slot:self"), f.events);
        f.events.clear();
        assertThrows(NullPointerException.class, () -> f.micro.edgeCornerOcclusionTest(null, null));
        assertEquals(Collections.singletonList("edge:self"), f.events);
    }

    @Test
    void getterFailuresStopBeforeLaterVirtualReads() throws Exception {
        Fixture f = new Fixture();
        f.configure(f.part, 7, 5, 0);
        f.configure(f.other, 10, 5, 1);
        String[] expected = { "super:self", "slot:self", "slot:other", "size:other", "size:self", "material:other",
                "material:self" };
        for (int i = 1; i < expected.length; i++) {
            String[] pieces = expected[i].split(":");
            TMultiPart failing = pieces[1].equals("self") ? f.part : f.other;
            f.set(failing, "failOn", String.class, pieces[0]);
            f.events.clear();
            assertEquals(
                    pieces[0],
                    assertThrows(IllegalStateException.class, () -> f.part.occlusionTest(f.other)).getMessage());
            assertEquals(Arrays.asList(Arrays.copyOf(expected, i + 1)), f.events);
            f.set(failing, "failOn", String.class, "");
        }
    }

    private static final class Fixture extends ClassLoader {

        final List<String> events = new ArrayList<>();
        final TMultiPart part;
        final TMultiPart other;
        final TMicroOcclusion micro;
        final TMicroOcclusion otherMicro;

        Fixture() throws Exception {
            super(TMultiPart.class.getClassLoader());
            for (String name : new String[] { "ReferenceMicroOcclusionBase", "ReferenceScalaTMicroOcclusion" }) {
                byte[] bytes;
                try (Scanner scanner = new Scanner(
                        getResourceAsStream("compat/" + name + ".class.b64"),
                        StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
                    bytes = Base64.getMimeDecoder().decode(scanner.next());
                }
                defineClass(null, bytes, 0, bytes.length);
            }
            Class<?> type = loadClass("codechicken.multipart.compat.ReferenceScalaTMicroOcclusion");
            part = (TMultiPart) type.getConstructor().newInstance();
            other = (TMultiPart) type.getConstructor().newInstance();
            micro = (TMicroOcclusion) part;
            otherMicro = (TMicroOcclusion) other;
            set(part, "events", List.class, events);
            set(other, "events", List.class, events);
            set(other, "label", String.class, "other");
        }

        void set(TMultiPart target, String name, Class<?> type, Object value) throws Exception {
            target.getClass().getMethod(name + "_$eq", type).invoke(target, value);
        }

        void configure(TMultiPart target, int slot, int size, int material) throws Exception {
            set(target, "slot", int.class, slot);
            set(target, "size", int.class, size);
            set(target, "material", int.class, material);
        }
    }
}
