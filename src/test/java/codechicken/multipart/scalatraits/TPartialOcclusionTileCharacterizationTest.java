package codechicken.multipart.scalatraits;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.JPartialOcclusion;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.collection.JavaConversions;
import scala.collection.Seq;

class TPartialOcclusionTileCharacterizationTest {

    @Test
    void keepsTheExactConcreteClassShapeConsumedByTheGenerator() {
        Class<TPartialOcclusionTile> type = TPartialOcclusionTile.class;
        assertEquals(TileMultipart.class, type.getSuperclass());
        assertArrayEquals(new Class<?>[0], type.getInterfaces());
        assertFalse(type.isInterface());
        assertFalse(Modifier.isAbstract(type.getModifiers()));
        assertFalse(Modifier.isFinal(type.getModifiers()));
        assertEquals(0, type.getDeclaredFields().length);

        Constructor<?>[] constructors = type.getDeclaredConstructors();
        assertEquals(1, constructors.length);
        assertEquals(0, constructors[0].getParameterTypes().length);
        assertTrue(Modifier.isPublic(constructors[0].getModifiers()));

        assertEquals(
                new TreeSet<>(
                        Arrays.asList(
                                "occlusionTest(scala.collection.Seq,codechicken.multipart.TMultiPart)boolean",
                                "partialOcclusionTest(scala.collection.Seq)boolean")),
                publicSignatures(type));
    }

    @Test
    void partialTestIgnoresNormalPartsAndRequiresExclusiveVoxels() {
        TPartialOcclusionTile tile = new TPartialOcclusionTile();

        assertTrue(tile.partialOcclusionTest(parts(new TestPart(true), new PartialPart(true, voxel(0)))));
        assertFalse(tile.partialOcclusionTest(parts(new PartialPart(true))));
        assertTrue(tile.partialOcclusionTest(parts(new PartialPart(true, true))));
        assertTrue(tile.partialOcclusionTest(parts(new PartialPart(true, voxel(0)), new PartialPart(true, voxel(1)))));
        assertFalse(tile.partialOcclusionTest(parts(new PartialPart(true, voxel(0)), new PartialPart(true, voxel(0)))));
    }

    @Test
    void partialCandidateIsCheckedBeforeTheNormalSuperChain() {
        TPartialOcclusionTile tile = new TPartialOcclusionTile();
        PartialPart existing = new PartialPart(true, voxel(0));
        PartialPart overlapping = new PartialPart(true, voxel(0));

        assertFalse(tile.occlusionTest(parts(existing), overlapping));
        assertEquals(0, calls(existing));
        assertEquals(0, calls(overlapping));

        PartialPart rejecting = new PartialPart(false, voxel(0));
        PartialPart disjoint = new PartialPart(true, voxel(1));
        assertFalse(tile.occlusionTest(parts(rejecting), disjoint));
        assertEquals(1, calls(rejecting));
        assertEquals(0, calls(disjoint));
    }

    @Test
    void nonPartialCandidateUsesTheNormalSuperChainDirectly() {
        TPartialOcclusionTile tile = new TPartialOcclusionTile();
        TestPart existing = new TestPart(true);
        TestPart candidate = new TestPart(false);

        assertFalse(tile.occlusionTest(parts(existing), candidate));
        assertEquals(1, existing.occlusionCalls);
        assertEquals(1, candidate.occlusionCalls);
    }

    private static Set<String> publicSignatures(Class<?> type) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                signatures.add(signature(method));
            }
        }
        return signatures;
    }

    private static String signature(Method method) {
        StringBuilder out = new StringBuilder(method.getName()).append('(');
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(parameters[i].getName());
        }
        return out.append(')').append(method.getReturnType().getName()).toString();
    }

    private static Seq<TMultiPart> parts(TMultiPart... parts) {
        return JavaConversions.asScalaBuffer(Arrays.asList(parts)).toSeq();
    }

    private static int calls(TestPart part) {
        return part.occlusionCalls;
    }

    private static Cuboid6 voxel(int x) {
        return new Cuboid6(x / 8d, 0, 0, (x + 1) / 8d, 1 / 8d, 1 / 8d);
    }

    private static class TestPart extends TMultiPart {

        private final boolean occlusionResult;
        private int occlusionCalls;

        private TestPart(boolean occlusionResult) {
            this.occlusionResult = occlusionResult;
        }

        @Override
        public String getType() {
            return "test:occlusion";
        }

        @Override
        public boolean occlusionTest(TMultiPart part) {
            occlusionCalls++;
            return occlusionResult;
        }
    }

    private static final class PartialPart extends TestPart implements JPartialOcclusion {

        private final boolean allowCompleteOcclusion;
        private final Iterable<Cuboid6> boxes;

        private PartialPart(boolean occlusionResult, Cuboid6... boxes) {
            this(occlusionResult, false, boxes);
        }

        private PartialPart(boolean occlusionResult, boolean allowCompleteOcclusion, Cuboid6... boxes) {
            super(occlusionResult);
            this.allowCompleteOcclusion = allowCompleteOcclusion;
            this.boxes = Collections.unmodifiableList(Arrays.asList(boxes));
        }

        @Override
        public Iterable<Cuboid6> getPartialOcclusionBoxes() {
            return boxes;
        }

        @Override
        public boolean allowCompleteOcclusion() {
            return allowCompleteOcclusion;
        }
    }
}
