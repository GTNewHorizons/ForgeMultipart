package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import codechicken.microblock.CornerMicroClass$;
import codechicken.microblock.EdgeMicroClass$;
import codechicken.microblock.FaceMicroClass$;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockGenerator;

class TMicroOcclusionFunctionalTest {

    @Test
    void generatedFacePairsRejectOnlyOpposingThicknessOverflowRegardlessOfMaterial() {
        int stone = MicroMaterialRegistry.materialID("minecraft:stone");
        int glass = MicroMaterialRegistry.materialID("minecraft:glass");
        assertNotEquals(stone, glass);
        Microblock first = MicroblockGenerator.create(FaceMicroClass$.MODULE$, stone, false);
        for (int material : new int[] { stone, glass }) {
            Microblock second = MicroblockGenerator.create(FaceMicroClass$.MODULE$, material, false);
            for (int side = 0; side < 6; side++) {
                for (int other = 0; other < 6; other++) {
                    for (int size = 1; size <= 7; size++) {
                        for (int otherSize = 1; otherSize <= 7; otherSize++) {
                            first.setShape(size, side);
                            second.setShape(otherSize, other);
                            boolean expected = other != (side ^ 1) || size + otherSize <= 8;
                            assertEquals(
                                    expected,
                                    first.occlusionTest(second),
                                    "sides=" + side + "," + other + " sizes=" + size + "," + otherSize);
                            assertEquals(expected, second.occlusionTest(first));
                        }
                    }
                }
            }
        }
    }

    @Test
    void generatedCornerAndEdgePairsKeepMaterialBoundariesAndAxisRulesInBothDirections() {
        int stone = MicroMaterialRegistry.materialID("minecraft:stone");
        int glass = MicroMaterialRegistry.materialID("minecraft:glass");
        assertNotEquals(stone, glass);
        Microblock corner = MicroblockGenerator.create(CornerMicroClass$.MODULE$, stone, false);
        Microblock edge = MicroblockGenerator.create(EdgeMicroClass$.MODULE$, stone, false);
        int[] masks = { 6, 5, 3 };
        int[] edgePositions = { 0, 2, 4, 6, 0, 4, 1, 5, 0, 1, 2, 3 };
        for (int material : new int[] { stone, glass }) {
            Microblock otherCorner = MicroblockGenerator.create(CornerMicroClass$.MODULE$, material, false);
            Microblock otherEdge = MicroblockGenerator.create(EdgeMicroClass$.MODULE$, material, false);
            for (int[] sizes : new int[][] { { 2, 6 }, { 3, 6 }, { 7, 7 } }) {
                boolean restricted = material != stone && sizes[0] + sizes[1] > 8;
                for (int c = 0; c < 8; c++) {
                    corner.setShape(sizes[0], 7 + c);
                    for (int other = 0; other < 8; other++) {
                        otherCorner.setShape(sizes[1], 7 + other);
                        int difference = Integer.bitCount(c ^ other);
                        assertBoth(!restricted || difference != 2, corner, otherCorner);
                    }
                    for (int e = 0; e < 12; e++) {
                        otherEdge.setShape(sizes[1], 15 + e);
                        assertBoth(!restricted || (c & masks[e / 4]) == edgePositions[e], corner, otherEdge);
                    }
                }
                for (int e = 0; e < 12; e++) {
                    edge.setShape(sizes[0], 15 + e);
                    for (int other = 0; other < 12; other++) {
                        otherEdge.setShape(sizes[1], 15 + other);
                        boolean oppositeParallel = e / 4 == other / 4 && (e % 4 + other % 4 == 3);
                        assertBoth(!restricted || !oppositeParallel, edge, otherEdge);
                    }
                }
            }
        }
    }

    private static void assertBoth(boolean expected, Microblock first, Microblock second) {
        assertEquals(expected, first.occlusionTest(second));
        assertEquals(expected, second.occlusionTest(first));
    }
}
