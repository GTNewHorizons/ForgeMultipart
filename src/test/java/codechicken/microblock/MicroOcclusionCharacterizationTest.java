package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

class MicroOcclusionCharacterizationTest {

    private static final int[] VALID_SLOTS = { 0, 1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, 26 };
    private static final Set<String> OCCLUSION_METHODS = signatures(
            "recalcBounds(Lcodechicken/microblock/JMicroShrinkRender;Lcodechicken/lib/vec/Cuboid6;)I",
            "shapePriority(I)I",
            "shrink(Lcodechicken/lib/vec/Cuboid6;Lcodechicken/lib/vec/Cuboid6;I)V",
            "shrink(Lcodechicken/microblock/JMicroShrinkRender;Lcodechicken/lib/vec/Cuboid6;I)I",
            "shrinkFrom(Lcodechicken/microblock/JMicroShrinkRender;Lcodechicken/microblock/JMicroShrinkRender;Lcodechicken/lib/vec/Cuboid6;)I",
            "shrinkSide(II)I",
            "shrinkTest(Lcodechicken/microblock/JMicroShrinkRender;Lcodechicken/microblock/JMicroShrinkRender;)Z");

    @Test
    void keepsFacadeCompanionAndGeneratedTraitHelperSurfaces() throws Exception {
        assertTrue(Modifier.isFinal(MicroOcclusion.class.getModifiers()));
        assertEquals(0, MicroOcclusion.class.getDeclaredFields().length);
        assertEquals(OCCLUSION_METHODS, publicDeclaredMethods(MicroOcclusion.class));
        for (Method method : MicroOcclusion.class.getDeclaredMethods()) {
            assertTrue(Modifier.isStatic(method.getModifiers()), method.toString());
        }

        assertTrue(Modifier.isFinal(MicroOcclusion$.class.getModifiers()));
        assertEquals(OCCLUSION_METHODS, publicDeclaredMethods(MicroOcclusion$.class));
        Field module = MicroOcclusion$.class.getField("MODULE$");
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertEquals(1, MicroOcclusion$.class.getDeclaredFields().length);

        assertTrait(
                JMicroShrinkRender.class,
                new Class<?>[0],
                signatures(
                        "getBounds()Lcodechicken/lib/vec/Cuboid6;",
                        "getPriorityClass()I",
                        "getSize()I",
                        "getSlot()I",
                        "isTransparent()Z"));
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("codechicken.microblock.JMicroShrinkRender$class"));

        assertTrait(
                TMicroOcclusion.class,
                new Class<?>[0],
                signatures(
                        "codechicken$microblock$TMicroOcclusion$$super$occlusionTest(Lcodechicken/multipart/TMultiPart;)Z",
                        "edgeCornerOcclusionTest(Lcodechicken/microblock/TMicroOcclusion;Lcodechicken/microblock/TMicroOcclusion;)Z",
                        "getBounds()Lcodechicken/lib/vec/Cuboid6;",
                        "getMaterial()I",
                        "getSize()I",
                        "getSlot()I",
                        "occlusionTest(Lcodechicken/multipart/TMultiPart;)Z"));
        assertHelper(
                "codechicken.microblock.TMicroOcclusion$class",
                signatures(
                        "$init$(Lcodechicken/microblock/TMicroOcclusion;)V",
                        "edgeCornerOcclusionTest(Lcodechicken/microblock/TMicroOcclusion;Lcodechicken/microblock/TMicroOcclusion;Lcodechicken/microblock/TMicroOcclusion;)Z",
                        "occlusionTest(Lcodechicken/microblock/TMicroOcclusion;Lcodechicken/multipart/TMultiPart;)Z"));

        assertTrait(
                TMicroOcclusionClient.class,
                new Class<?>[] { TMicroOcclusion.class, JMicroShrinkRender.class },
                signatures(
                        "codechicken$microblock$TMicroOcclusionClient$$super$onAdded()V",
                        "codechicken$microblock$TMicroOcclusionClient$$super$onPartChanged(Lcodechicken/multipart/TMultiPart;)V",
                        "codechicken$microblock$TMicroOcclusionClient$$super$read(Lcodechicken/lib/data/MCDataInput;)V",
                        "getPriorityClass()I",
                        "onAdded()V",
                        "onPartChanged(Lcodechicken/multipart/TMultiPart;)V",
                        "read(Lcodechicken/lib/data/MCDataInput;)V",
                        "recalcBounds()V",
                        "renderBounds()Lcodechicken/lib/vec/Cuboid6;",
                        "renderBounds_$eq(Lcodechicken/lib/vec/Cuboid6;)V",
                        "renderMask()I",
                        "renderMask_$eq(I)V"));
        assertHelper(
                "codechicken.microblock.TMicroOcclusionClient$class",
                signatures(
                        "$init$(Lcodechicken/microblock/TMicroOcclusionClient;)V",
                        "getPriorityClass(Lcodechicken/microblock/TMicroOcclusionClient;)I",
                        "onAdded(Lcodechicken/microblock/TMicroOcclusionClient;)V",
                        "onPartChanged(Lcodechicken/microblock/TMicroOcclusionClient;Lcodechicken/multipart/TMultiPart;)V",
                        "read(Lcodechicken/microblock/TMicroOcclusionClient;Lcodechicken/lib/data/MCDataInput;)V",
                        "recalcBounds(Lcodechicken/microblock/TMicroOcclusionClient;)V"));

        assertTrue(
                TMicroOcclusion.class.getDeclaredMethod(
                        "codechicken$microblock$TMicroOcclusion$$super$occlusionTest",
                        TMultiPart.class).isSynthetic());
        assertTrue(
                TMicroOcclusionClient.class
                        .getDeclaredMethod("codechicken$microblock$TMicroOcclusionClient$$super$onAdded")
                        .isSynthetic());
        assertTrue(
                TMicroOcclusionClient.class.getDeclaredMethod(
                        "codechicken$microblock$TMicroOcclusionClient$$super$onPartChanged",
                        TMultiPart.class).isSynthetic());
    }

    @Test
    void freezesAllValidSlotPriorityAndShrinkDecisions() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int slot : VALID_SLOTS) {
            int priority = MicroOcclusion.shapePriority(slot);
            assertEquals(priority, MicroOcclusion$.MODULE$.shapePriority(slot));
            update(digest, priority);
        }
        for (int first : VALID_SLOTS) {
            for (int second : VALID_SLOTS) {
                int side = MicroOcclusion.shrinkSide(first, second);
                assertEquals(side, MicroOcclusion$.MODULE$.shrinkSide(first, second));
                update(digest, side);
            }
        }

        int[] priorities = { -1, 0, 1 };
        int[] sizes = { 1, 4, 7 };
        for (int firstSlot : VALID_SLOTS) {
            for (int secondSlot : VALID_SLOTS) {
                for (int firstPriority : priorities) {
                    for (int secondPriority : priorities) {
                        for (int firstSize : sizes) {
                            for (int secondSize : sizes) {
                                for (boolean firstTransparent : new boolean[] { false, true }) {
                                    for (boolean secondTransparent : new boolean[] { false, true }) {
                                        TestPart first = new TestPart(
                                                firstSlot,
                                                firstSize,
                                                firstPriority,
                                                firstTransparent,
                                                Cuboid6.full);
                                        TestPart second = new TestPart(
                                                secondSlot,
                                                secondSize,
                                                secondPriority,
                                                secondTransparent,
                                                Cuboid6.full);
                                        boolean shrinks = MicroOcclusion.shrinkTest(first, second);
                                        assertEquals(shrinks, MicroOcclusion$.MODULE$.shrinkTest(first, second));
                                        digest.update((byte) (shrinks ? 1 : 0));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        assertEquals("89add86e47ae62da740d85b76193fbd10e791edcdf53fb3c48737349b2483abe", hex(digest.digest()));
    }

    @Test
    void shrinksEachAxisWithoutExpandingExistingBounds() {
        Cuboid6 obstacle = new Cuboid6(0.2, 0.3, 0.4, 0.8, 0.7, 0.6);
        Cuboid6[] expected = { Cuboid6.full.copy(), new Cuboid6(0, 0.7, 0, 1, 1, 1), new Cuboid6(0, 0, 0, 1, 0.3, 1),
                new Cuboid6(0, 0, 0.6, 1, 1, 1), new Cuboid6(0, 0, 0, 1, 1, 0.4), new Cuboid6(0.8, 0, 0, 1, 1, 1),
                new Cuboid6(0, 0, 0, 0.2, 1, 1) };
        for (int side = -1; side < 6; side++) {
            Cuboid6 actual = Cuboid6.full.copy();
            MicroOcclusion.shrink(actual, obstacle, side);
            assertCuboid(expected[side + 1], actual);
        }

        Cuboid6 alreadyInside = new Cuboid6(0.9, 0.8, 0.7, 0.1, 0.2, 0.3);
        Cuboid6 original = alreadyInside.copy();
        for (int side = 0; side < 6; side++) {
            MicroOcclusion.shrink(alreadyInside, obstacle, side);
        }
        assertCuboid(original, alreadyInside);
    }

    @Test
    void keepsShrinkMaskAndSlotTraversalRules() {
        TestPart shrinking = new TestPart(15, 4, 0, false, Cuboid6.full);
        TestPart face = new TestPart(0, 4, 1, false, new Cuboid6(0, 0, 0, 1, 0.25, 1));
        Cuboid6 bounds = Cuboid6.full.copy();
        assertEquals(0, MicroOcclusion.shrinkFrom(shrinking, face, bounds));
        assertEquals(0.25, bounds.min.y);

        TestPart yielding = new TestPart(15, 4, 1, false, Cuboid6.full);
        TestPart opaqueFace = new TestPart(0, 4, 0, false, Cuboid6.full);
        assertEquals(1, MicroOcclusion.shrinkFrom(yielding, opaqueFace, Cuboid6.full.copy()));
        opaqueFace.transparent = true;
        assertEquals(0, MicroOcclusion.shrinkFrom(yielding, opaqueFace, Cuboid6.full.copy()));
        opaqueFace.transparent = false;
        assertEquals(0, MicroOcclusion.shrinkFrom(yielding, opaqueFace, new Cuboid6(0, 0.1, 0, 1, 1, 1)));

        RecordingTile tile = new RecordingTile();
        shrinking.bind(tile);
        tile.parts[0] = face;
        bounds = Cuboid6.full.copy();
        assertEquals(0, MicroOcclusion.shrink(shrinking, bounds, 6));
        assertEquals(0.25, bounds.min.y);

        tile.parts[0] = null;
        assertTraversal(shrinking, tile, 0, 5, 5);
        assertTraversal(shrinking, tile, 7, 14, 14);
        assertTraversal(shrinking, tile, 15, 26, 26);
    }

    @Test
    void freezesCompleteMicroblockOcclusionDecisions() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        TestOcclusionPart first = new TestOcclusionPart();
        TestOcclusionPart second = new TestOcclusionPart();
        for (int firstSlot : VALID_SLOTS) {
            for (int secondSlot : VALID_SLOTS) {
                for (int firstSize = 1; firstSize < 8; firstSize++) {
                    for (int secondSize = 1; secondSize < 8; secondSize++) {
                        for (int secondMaterial = 0; secondMaterial < 2; secondMaterial++) {
                            first.set(firstSlot, firstSize, 0);
                            second.set(secondSlot, secondSize, secondMaterial);
                            digest.update((byte) (first.occlusionTest(second) ? 1 : 0));
                        }
                    }
                }
            }
        }
        for (int edge = 15; edge < 27; edge++) {
            for (int corner = 7; corner < 15; corner++) {
                first.set(edge, 4, 0);
                second.set(corner, 4, 1);
                digest.update((byte) (first.edgeCornerOcclusionTest(first, second) ? 1 : 0));
            }
        }

        assertEquals("089ef2772b6f50b07b8c09a1733bc0e2a6e2006c134095b19f797b0b37ceb55b", hex(digest.digest()));
        assertTrue(first.occlusionTest(new BarePart()));
        first.superResult = false;
        assertFalse(first.occlusionTest(second));
    }

    private static void assertTraversal(TestPart part, RecordingTile tile, int slot, int lastSlot, int calls) {
        part.slot = slot;
        tile.visited.clear();
        assertEquals(0, MicroOcclusion.recalcBounds(part, Cuboid6.full.copy()));
        assertEquals(calls, tile.visited.size());
        assertEquals(lastSlot, tile.visited.get(tile.visited.size() - 1));
        assertFalse(tile.visited.contains(slot));
    }

    private static void assertTrait(Class<?> type, Class<?>[] interfaces, Set<String> methods) {
        assertTrue(type.isInterface());
        assertArrayEquals(interfaces, type.getInterfaces());
        assertEquals(0, type.getDeclaredFields().length);
        assertEquals(methods, publicDeclaredMethods(type));
    }

    private static void assertHelper(String name, Set<String> methods) throws Exception {
        Class<?> type = Class.forName(name, false, MicroOcclusionCharacterizationTest.class.getClassLoader());
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isAbstract(type.getModifiers()));
        assertEquals(0, type.getDeclaredFields().length);
        assertEquals(methods, publicDeclaredMethods(type));
    }

    private static Set<String> publicDeclaredMethods(Class<?> type) {
        Set<String> methods = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                methods.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return methods;
    }

    private static Set<String> signatures(String... signatures) {
        return new TreeSet<>(Arrays.asList(signatures));
    }

    private static void update(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit(value >>> 4 & 0xF, 16));
            result.append(Character.forDigit(value & 0xF, 16));
        }
        return result.toString();
    }

    private static void assertCuboid(Cuboid6 expected, Cuboid6 actual) {
        assertEquals(expected.min.x, actual.min.x);
        assertEquals(expected.min.y, actual.min.y);
        assertEquals(expected.min.z, actual.min.z);
        assertEquals(expected.max.x, actual.max.x);
        assertEquals(expected.max.y, actual.max.y);
        assertEquals(expected.max.z, actual.max.z);
    }

    private static class TestPart extends TMultiPart implements JMicroShrinkRender {

        private int slot;
        private final int size;
        private final int priority;
        private boolean transparent;
        private final Cuboid6 bounds;

        private TestPart(int slot, int size, int priority, boolean transparent, Cuboid6 bounds) {
            this.slot = slot;
            this.size = size;
            this.priority = priority;
            this.transparent = transparent;
            this.bounds = bounds;
        }

        @Override
        public String getType() {
            return "test_shrink";
        }

        @Override
        public int getPriorityClass() {
            return priority;
        }

        @Override
        public int getSlot() {
            return slot;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public boolean isTransparent() {
            return transparent;
        }

        @Override
        public Cuboid6 getBounds() {
            return bounds;
        }
    }

    private static final class TestOcclusionPart extends TMultiPart implements TMicroOcclusion {

        private int slot;
        private int size;
        private int material;
        private boolean superResult = true;

        private void set(int slot, int size, int material) {
            this.slot = slot;
            this.size = size;
            this.material = material;
            superResult = true;
        }

        @Override
        public String getType() {
            return "test_occlusion";
        }

        @Override
        public int getSlot() {
            return slot;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public int getMaterial() {
            return material;
        }

        @Override
        public Cuboid6 getBounds() {
            return Cuboid6.full;
        }

        @Override
        public boolean occlusionTest(TMultiPart part) {
            return TMicroOcclusion$class.occlusionTest(this, part);
        }

        @Override
        public boolean edgeCornerOcclusionTest(TMicroOcclusion edge, TMicroOcclusion corner) {
            return TMicroOcclusion$class.edgeCornerOcclusionTest(this, edge, corner);
        }

        public boolean codechicken$microblock$TMicroOcclusion$$super$occlusionTest(TMultiPart part) {
            return superResult;
        }
    }

    private static final class BarePart extends TMultiPart {

        @Override
        public String getType() {
            return "bare";
        }
    }

    private static final class RecordingTile extends TileMultipart {

        private final TMultiPart[] parts = new TMultiPart[27];
        private final List<Integer> visited = new ArrayList<>();

        @Override
        public TMultiPart partMap(int slot) {
            visited.add(slot);
            return parts[slot];
        }
    }
}
