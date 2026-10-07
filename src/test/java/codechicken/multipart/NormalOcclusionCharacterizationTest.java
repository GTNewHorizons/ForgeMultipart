package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;
import scala.collection.Traversable;
import scala.collection.mutable.ArrayBuffer;

class NormalOcclusionCharacterizationTest {

    @Test
    void emptyInputsAreCompatible() {
        Cuboid6 box = box(0, 0, 0, 1, 1, 1);

        assertTrue(NormalOcclusionTest.apply(scalaBoxes(), scalaBoxes()));
        assertTrue(NormalOcclusionTest.apply(scalaBoxes(box), scalaBoxes()));
        assertTrue(NormalOcclusionTest.apply(scalaBoxes(), scalaBoxes(box)));
    }

    @Test
    void separationOnAnyAxisIsCompatible() {
        Cuboid6 origin = box(0, 0, 0, 0.25, 0.25, 0.25);
        Cuboid6[] separated = { box(0.5, 0, 0, 0.75, 0.25, 0.25), box(0, 0.5, 0, 0.25, 0.75, 0.25),
                box(0, 0, 0.5, 0.25, 0.25, 0.75) };

        for (Cuboid6 other : separated) {
            assertTrue(compatible(origin, other));
            assertTrue(compatible(other, origin));
        }
    }

    @Test
    void touchingAndSubToleranceOverlapAreCompatible() {
        Cuboid6 origin = box(0, 0, 0, 0.5, 1, 1);

        assertTrue(compatible(origin, box(0.5, 0, 0, 1, 1, 1)));
        assertTrue(compatible(origin, box(0.499995, 0, 0, 1, 1, 1)));
        assertFalse(compatible(origin, box(0.49998, 0, 0, 1, 1, 1)));
    }

    @Test
    void partialOverlapContainmentAndIdentityAreIncompatible() {
        Cuboid6 origin = box(0, 0, 0, 0.75, 0.75, 0.75);

        assertFalse(compatible(origin, box(0.5, 0.5, 0.5, 1, 1, 1)));
        assertFalse(compatible(origin, box(0.1, 0.1, 0.1, 0.2, 0.2, 0.2)));
        assertFalse(compatible(origin, box(0, 0, 0, 0.75, 0.75, 0.75)));
    }

    @Test
    void everyBoxPairMustBeCompatible() {
        Traversable<Cuboid6> existing = scalaBoxes(box(0, 0, 0, 0.2, 0.2, 0.2), box(0.8, 0.8, 0.8, 1, 1, 1));

        assertTrue(NormalOcclusionTest.apply(existing, scalaBoxes(box(0.3, 0.3, 0.3, 0.4, 0.4, 0.4))));
        assertFalse(
                NormalOcclusionTest.apply(
                        existing,
                        scalaBoxes(box(0.3, 0.3, 0.3, 0.4, 0.4, 0.4), box(0.9, 0.9, 0.9, 0.95, 0.95, 0.95))));
    }

    @Test
    void normallyOccludedPartsUseTheSameRule() {
        NormallyOccludedPart origin = new NormallyOccludedPart(box(0, 0, 0, 0.5, 1, 1));
        NormallyOccludedPart touching = new NormallyOccludedPart(box(0.5, 0, 0, 1, 1, 1));
        NormallyOccludedPart overlapping = new NormallyOccludedPart(box(0.25, 0, 0, 0.75, 1, 1));

        assertTrue(origin.occlusionTest(touching));
        assertTrue(touching.occlusionTest(origin));
        assertFalse(origin.occlusionTest(overlapping));
        assertFalse(overlapping.occlusionTest(origin));
    }

    @Test
    void partHelperIncludesPartialOcclusionBoxes() {
        NormallyOccludedPart origin = new NormallyOccludedPart(box(0, 0, 0, 0.5, 1, 1));
        PartialPart touching = new PartialPart(box(0.5, 0, 0, 1, 1, 1));
        PartialPart overlapping = new PartialPart(box(0.25, 0, 0, 0.75, 1, 1));

        assertTrue(NormalOcclusionTest.apply(origin, touching));
        assertFalse(NormalOcclusionTest.apply(origin, overlapping));
    }

    @Test
    void partWithoutOcclusionInterfacesIsCompatible() {
        NormallyOccludedPart origin = new NormallyOccludedPart(box(0, 0, 0, 1, 1, 1));
        TMultiPart barePart = new TMultiPart() {

            @Override
            public String getType() {
                return "test:bare";
            }
        };

        assertTrue(NormalOcclusionTest.apply(origin, barePart));
    }

    @Test
    void singletonAndStaticForwardersAgree() {
        Traversable<Cuboid6> left = scalaBoxes(box(0, 0, 0, 0.5, 1, 1));
        Traversable<Cuboid6> fits = scalaBoxes(box(0.5, 0, 0, 1, 1, 1));
        Traversable<Cuboid6> overlaps = scalaBoxes(box(0.25, 0, 0, 1, 1, 1));

        assertTrue(NormalOcclusionTest$.MODULE$.apply(left, fits));
        assertFalse(NormalOcclusionTest$.MODULE$.apply(left, overlaps));

        NormallyOccludedPart part = new NormallyOccludedPart(box(0, 0, 0, 0.5, 1, 1));
        NormallyOccludedPart neighbour = new NormallyOccludedPart(box(0.5, 0, 0, 1, 1, 1));
        assertEquals(NormalOcclusionTest.apply(part, neighbour), NormalOcclusionTest$.MODULE$.apply(part, neighbour));
    }

    @Test
    void bridgeCombinesTheBoxTestWithTheSuperChain() {
        NormallyOccludedPart part = new NormallyOccludedPart(box(0, 0, 0, 0.5, 1, 1));
        TMultiPart fits = new NormallyOccludedPart(box(0.5, 0, 0, 1, 1, 1));
        TMultiPart overlaps = new NormallyOccludedPart(box(0.25, 0, 0, 1, 1, 1));

        assertTrue(TNormalOcclusion$class.occlusionTest(part, fits));
        assertFalse(TNormalOcclusion$class.occlusionTest(part, overlaps));
    }

    /**
     * The super accessor is ACC_SYNTHETIC in the reference, so javac hides it and only reflection can reach it. The
     * bridge calls it through invokeinterface, so its name and descriptor are part of the supported ABI.
     */
    @Test
    void superAccessorExistsAndChainsToTheTMultiPartDefault() throws Exception {
        NormallyOccludedPart part = new NormallyOccludedPart(box(0, 0, 0, 0.5, 1, 1));
        Method accessor = TNormalOcclusion.class
                .getMethod("codechicken$multipart$TNormalOcclusion$$super$occlusionTest", TMultiPart.class);

        assertEquals(Boolean.TRUE, accessor.invoke(part, new NormallyOccludedPart(box(0.25, 0, 0, 1, 1, 1))));
        assertNull(part.getType());
    }

    @Test
    void singleBoxConstructorMatchesTheIterableConstructor() {
        Cuboid6 bound = box(0, 0, 0, 0.5, 1, 1);
        NormallyOccludedPart single = new NormallyOccludedPart(bound);
        TMultiPart overlaps = new NormallyOccludedPart(box(0.25, 0, 0, 1, 1, 1));

        List<Cuboid6> boxes = new ArrayList<>();
        for (Cuboid6 box : single.getOcclusionBoxes()) {
            boxes.add(box);
        }

        assertEquals(1, boxes.size());
        assertSame(bound, boxes.get(0));
        assertFalse(single.occlusionTest(overlaps));
    }

    private static boolean compatible(Cuboid6 first, Cuboid6 second) {
        return NormalOcclusionTest.apply(scalaBoxes(first), scalaBoxes(second));
    }

    private static Traversable<Cuboid6> scalaBoxes(Cuboid6... boxes) {
        ArrayBuffer<Cuboid6> result = new ArrayBuffer<>();
        for (Cuboid6 box : boxes) {
            result.$plus$eq(box);
        }
        return result;
    }

    private static Cuboid6 box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new Cuboid6(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static final class PartialPart extends TMultiPart implements JPartialOcclusion {

        private final Iterable<Cuboid6> boxes;

        private PartialPart(Cuboid6... boxes) {
            this.boxes = Collections.unmodifiableList(Arrays.asList(boxes));
        }

        @Override
        public String getType() {
            return "test:partial";
        }

        @Override
        public Iterable<Cuboid6> getPartialOcclusionBoxes() {
            return boxes;
        }

        @Override
        public boolean allowCompleteOcclusion() {
            return false;
        }
    }
}
