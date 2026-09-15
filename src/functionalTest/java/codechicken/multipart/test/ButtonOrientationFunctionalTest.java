package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.examples.ButtonOrientationExample;
import codechicken.multipart.minecraft.ButtonPart;

class ButtonOrientationFunctionalTest {

    private static final BlockCoord BUTTON = new BlockCoord(64, 200, 48);

    @Test
    void etFuturumArrayMutationEnablesFloorAndCeilingPlacement() {
        World world = MinecraftServer.getServer().worldServers[0];
        world.getChunkFromBlockCoords(BUTTON.x, BUTTON.z);
        int[] originalMetaSideMap = ButtonPart.metaSideMap;
        int[] originalSideMetaMap = ButtonPart.sideMetaMap;
        int[] originalMetaSideValues = originalMetaSideMap.clone();
        int[] originalSideMetaValues = originalSideMetaMap.clone();

        try {
            assertArrayEquals(new int[] { -1, 4, 5, 2, 3, -1, -1, -1 }, ButtonPart.metaSideMap);
            assertArrayEquals(new int[] { -1, -1, 3, 4, 1, 2 }, ButtonPart.sideMetaMap);
            prepareSupports(world);
            assertNull(ButtonPart.placement(world, BUTTON, 0, 0));
            assertNull(ButtonPart.placement(world, BUTTON, 1, 0));

            int[] metaSideMap = ButtonPart.metaSideMap;
            metaSideMap[0] = 1;
            metaSideMap[5] = 0;
            ButtonPart.metaSideMap = metaSideMap;
            int[] sideMetaMap = ButtonPart.sideMetaMap;
            sideMetaMap[0] = 5;
            sideMetaMap[1] = 0;
            ButtonPart.sideMetaMap = sideMetaMap;

            assertSame(metaSideMap, ButtonPart.metaSideMap);
            assertSame(sideMetaMap, ButtonPart.sideMetaMap);
            assertArrayEquals(new int[] { 1, 4, 5, 2, 3, 0, -1, -1 }, ButtonPart.metaSideMap);
            assertArrayEquals(new int[] { 5, 0, 3, 4, 1, 2 }, ButtonPart.sideMetaMap);

            int[] expectedMetadata = { 0, 5, 4, 3, 2, 1 };
            for (int side = 0; side < 6; side++) {
                ButtonPart part = (ButtonPart) ButtonPart.placement(world, BUTTON, side, 0);
                assertEquals(expectedMetadata[side], part.getMetadata());
                assertEquals(side ^ 1, part.getFace());
            }
        } finally {
            clear(world);
            System.arraycopy(originalMetaSideValues, 0, originalMetaSideMap, 0, originalMetaSideMap.length);
            System.arraycopy(originalSideMetaValues, 0, originalSideMetaMap, 0, originalSideMetaMap.length);
            ButtonPart.metaSideMap = originalMetaSideMap;
            ButtonPart.sideMetaMap = originalSideMetaMap;
        }
    }

    @Test
    void typedApiMatchesEtFuturumAndKeepsMappingsOneToOne() {
        int[] originalMetaSideMap = ButtonPart.metaSideMap;
        int[] originalSideMetaMap = ButtonPart.sideMetaMap;
        int[] originalMetaSideValues = originalMetaSideMap.clone();
        int[] originalSideMetaValues = originalSideMetaMap.clone();

        try {
            ButtonOrientationExample.registerVerticalOrientations();
            assertArrayEquals(new int[] { 1, 4, 5, 2, 3, 0, -1, -1 }, ButtonPart.metaSideMap);
            assertArrayEquals(new int[] { 5, 0, 3, 4, 1, 2 }, ButtonPart.sideMetaMap);

            ButtonPart.setOrientation(1, ForgeDirection.DOWN);
            assertArrayEquals(new int[] { 1, 0, 5, 2, 3, -1, -1, -1 }, ButtonPart.metaSideMap);
            assertArrayEquals(new int[] { 1, 0, 3, 4, -1, 2 }, ButtonPart.sideMetaMap);

            int[] metaSideValues = ButtonPart.metaSideMap.clone();
            int[] sideMetaValues = ButtonPart.sideMetaMap.clone();
            assertThrows(IllegalArgumentException.class, () -> ButtonPart.setOrientation(-1, ForgeDirection.UP));
            assertThrows(IllegalArgumentException.class, () -> ButtonPart.setOrientation(8, ForgeDirection.UP));
            assertThrows(IllegalArgumentException.class, () -> ButtonPart.setOrientation(0, null));
            assertThrows(IllegalArgumentException.class, () -> ButtonPart.setOrientation(0, ForgeDirection.UNKNOWN));
            assertArrayEquals(metaSideValues, ButtonPart.metaSideMap);
            assertArrayEquals(sideMetaValues, ButtonPart.sideMetaMap);
        } finally {
            System.arraycopy(originalMetaSideValues, 0, originalMetaSideMap, 0, originalMetaSideMap.length);
            System.arraycopy(originalSideMetaValues, 0, originalSideMetaMap, 0, originalSideMetaMap.length);
            ButtonPart.metaSideMap = originalMetaSideMap;
            ButtonPart.sideMetaMap = originalSideMetaMap;
        }
    }

    private static void prepareSupports(World world) {
        clear(world);
        for (int side = 0; side < 6; side++) {
            BlockCoord support = BUTTON.copy().offset(side);
            assertTrue(world.setBlock(support.x, support.y, support.z, Blocks.stone, 0, 3));
        }
    }

    private static void clear(World world) {
        world.setBlockToAir(BUTTON.x, BUTTON.y, BUTTON.z);
        for (int side = 0; side < 6; side++) {
            BlockCoord support = BUTTON.copy().offset(side);
            world.setBlockToAir(support.x, support.y, support.z);
        }
    }
}
