package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.PartMap;

class PlacementGridsCharacterizationTest {

    private static final Set<String> GRID_METHODS = signatures(
            "drawLines()V",
            "getHitSlot(Lcodechicken/lib/vec/Vector3;I)I",
            "glTransformFace(Lcodechicken/lib/vec/Vector3;I)V",
            "render(Lcodechicken/lib/vec/Vector3;I)V");
    private static final Set<String> HELPER_METHODS = signatures(
            "$init$(Lcodechicken/microblock/PlacementGrid;)V",
            "drawLines(Lcodechicken/microblock/PlacementGrid;)V",
            "glTransformFace(Lcodechicken/microblock/PlacementGrid;Lcodechicken/lib/vec/Vector3;I)V",
            "render(Lcodechicken/microblock/PlacementGrid;Lcodechicken/lib/vec/Vector3;I)V");

    @Test
    void keepsTheNineClassHierarchyAndCallableSurface() throws Exception {
        assertTrue(PlacementGrid.class.isInterface());
        assertEquals(GRID_METHODS, publicDeclaredMethods(PlacementGrid.class));

        assertSame(Object.class, PlacementGrid$class.class.getSuperclass());
        assertTrue(Modifier.isPublic(PlacementGrid$class.class.getModifiers()));
        assertTrue(Modifier.isAbstract(PlacementGrid$class.class.getModifiers()));
        assertEquals(HELPER_METHODS, publicDeclaredMethods(PlacementGrid$class.class));

        assertSame(Object.class, FaceEdgeGrid.class.getSuperclass());
        assertEquals(Arrays.asList(PlacementGrid.class), Arrays.asList(FaceEdgeGrid.class.getInterfaces()));
        assertEquals(GRID_METHODS, publicDeclaredMethods(FaceEdgeGrid.class));
        assertTrue(Modifier.isPublic(FaceEdgeGrid.class.getConstructor(double.class).getModifiers()));
        Field size = FaceEdgeGrid.class.getDeclaredField("size");
        assertSame(double.class, size.getType());
        assertTrue(Modifier.isPrivate(size.getModifiers()));
        assertTrue(Modifier.isFinal(size.getModifiers()));

        assertCompanion(FacePlacementGrid$.class, FaceEdgeGrid.class, signatures(), "MODULE$");
        assertCompanion(CornerPlacementGrid$.class, Object.class, GRID_METHODS, "MODULE$");
        assertCompanion(EdgePlacementGrid$.class, Object.class, GRID_METHODS, "MODULE$");
        assertFacade(FacePlacementGrid.class);
        assertFacade(CornerPlacementGrid.class);
        assertFacade(EdgePlacementGrid.class);

        assertSame(FacePlacementGrid$.MODULE$, FacePlacementGrid$.class.getField("MODULE$").get(null));
        assertSame(CornerPlacementGrid$.MODULE$, CornerPlacementGrid$.class.getField("MODULE$").get(null));
        assertSame(EdgePlacementGrid$.MODULE$, EdgePlacementGrid$.class.getField("MODULE$").get(null));
    }

    @Test
    void faceGridKeepsCenterAxisTieAndStrictBoundarySelection() {
        for (int side = 0; side < 6; side++) {
            int s1 = (side + 2) % 6;
            int s2 = (side + 4) % 6;
            assertFaceSlot(side ^ 1, side, 0, 0);
            assertFaceSlot(side ^ 1, side, 0.249999, 0);
            assertFaceSlot(s1, side, 0.25, 0);
            assertFaceSlot(s1, side, 0.3, 0);
            assertFaceSlot(s1 ^ 1, side, -0.3, 0);
            assertFaceSlot(s2, side, 0, 0.3);
            assertFaceSlot(s2 ^ 1, side, 0, -0.3);
            assertFaceSlot(s2, side, 0.3, 0.3);
        }
    }

    @Test
    void faceEdgeGridKeepsItsConfiguredCenterSize() {
        FaceEdgeGrid hollowGrid = new FaceEdgeGrid(3 / 8d);
        for (int side = 0; side < 6; side++) {
            int s1 = (side + 2) % 6;
            assertEquals(side ^ 1, hollowGrid.getHitSlot(point(side, 0.374999, 0), side));
            assertEquals(s1, hollowGrid.getHitSlot(point(side, 0.375, 0), side));
        }
    }

    @Test
    void cornerGridKeepsAllQuadrantsForEveryHitSide() {
        int[][] expected = { { 8, 12, 10, 14 }, { 7, 11, 9, 13 }, { 9, 10, 13, 14 }, { 7, 8, 11, 12 },
                { 11, 13, 12, 14 }, { 7, 9, 8, 10 } };
        for (int side = 0; side < 6; side++) {
            int index = 0;
            for (double u : new double[] { -0.2, 0.2 }) {
                for (double v : new double[] { -0.2, 0.2 }) {
                    assertEquals(
                            expected[side][index++],
                            CornerPlacementGrid.getHitSlot(cornerPoint(side, u, v), side));
                }
            }
        }
    }

    @Test
    void edgeGridKeepsCenterSingleAxisDiagonalAndBoundarySelection() {
        for (int side = 0; side < 6; side++) {
            int s1 = (side + 2) % 6;
            int s2 = (side + 4) % 6;
            assertEdgeSlot(-1, side, 0, 0);
            assertEdgeSlot(-1, side, 0.249999, 0.249999);
            assertEdgeSlot(PartMap.edgeBetween(side ^ 1, s2), side, 0.25, 0.25);
            assertEdgeSlot(PartMap.edgeBetween(side ^ 1, s1), side, 0.3, 0);
            assertEdgeSlot(PartMap.edgeBetween(side ^ 1, s1 ^ 1), side, -0.3, 0);
            assertEdgeSlot(PartMap.edgeBetween(side ^ 1, s2), side, 0, 0.3);
            assertEdgeSlot(PartMap.edgeBetween(side ^ 1, s2 ^ 1), side, 0, -0.3);
            assertEdgeSlot(PartMap.edgeBetween(s1, s2), side, 0.3, 0.3);
            assertEdgeSlot(PartMap.edgeBetween(s1 ^ 1, s2), side, -0.3, 0.3);
            assertEdgeSlot(PartMap.edgeBetween(s1, s2 ^ 1), side, 0.3, -0.3);
            assertEdgeSlot(PartMap.edgeBetween(s1 ^ 1, s2 ^ 1), side, -0.3, -0.3);
        }
    }

    private static void assertFaceSlot(int expected, int side, double u, double v) {
        Vector3 hit = point(side, u, v);
        assertEquals(expected, FacePlacementGrid.getHitSlot(hit, side));
        assertEquals(expected, FacePlacementGrid$.MODULE$.getHitSlot(hit, side));
    }

    private static void assertEdgeSlot(int expected, int side, double u, double v) {
        Vector3 hit = point(side, u, v);
        assertEquals(expected, EdgePlacementGrid.getHitSlot(hit, side));
        assertEquals(expected, EdgePlacementGrid$.MODULE$.getHitSlot(hit, side));
    }

    private static Vector3 point(int side, double u, double v) {
        int s1 = (side + 2) % 6;
        int s2 = (side + 4) % 6;
        return point(s1, s2, u, v);
    }

    private static Vector3 cornerPoint(int side, double u, double v) {
        int s1 = ((side & 6) + 3) % 6;
        int s2 = ((side & 6) + 5) % 6;
        return point(s1, s2, u, v);
    }

    private static Vector3 point(int s1, int s2, double u, double v) {
        Vector3 axis1 = Rotation.axes[s1];
        Vector3 axis2 = Rotation.axes[s2];
        return new Vector3(
                0.5 + axis1.x * u + axis2.x * v,
                0.5 + axis1.y * u + axis2.y * v,
                0.5 + axis1.z * u + axis2.z * v);
    }

    private static void assertCompanion(Class<?> type, Class<?> superclass, Set<String> methods, String moduleName)
            throws Exception {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertSame(superclass, type.getSuperclass());
        assertEquals(methods, publicDeclaredMethods(type));
        Field module = type.getField(moduleName);
        assertSame(type, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
    }

    private static void assertFacade(Class<?> type) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertSame(Object.class, type.getSuperclass());
        assertEquals(
                signatures(
                        "drawLines()V",
                        "getHitSlot(Lcodechicken/lib/vec/Vector3;I)I",
                        "glTransformFace(Lcodechicken/lib/vec/Vector3;I)V",
                        "render(Lcodechicken/lib/vec/Vector3;I)V"),
                publicDeclaredMethods(type));
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                assertTrue(Modifier.isStatic(method.getModifiers()));
            }
        }
    }

    private static Set<String> signatures(String... signatures) {
        return new TreeSet<>(Arrays.asList(signatures));
    }

    private static Set<String> publicDeclaredMethods(Class<?> type) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return signatures;
    }
}
