package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;

/**
 * Placement short-circuits as soon as newPart returns null, before any world, player or stack access, so the branch and
 * offset logic is reachable headless with null arguments. Actual placement needs a world and stays on the functional
 * and manual layers.
 */
class ItemMultiPartCharacterizationTest {

    @Test
    void hitDepthProjectsOntoTheSideAxisAndOffsetsOddSides() {
        SpyItem item = new SpyItem();
        Vector3 vhit = new Vector3(0.25, 0.5, 0.75);

        assertEquals(0.5, item.getHitDepth(vhit, 0));
        assertEquals(0.5, item.getHitDepth(vhit, 1));
        assertEquals(0.25, item.getHitDepth(vhit, 2));
        assertEquals(0.75, item.getHitDepth(vhit, 3));
        assertEquals(0.75, item.getHitDepth(vhit, 4));
        assertEquals(0.25, item.getHitDepth(vhit, 5));
    }

    @Test
    void hitDepthDoesNotMutateTheSuppliedVector() {
        SpyItem item = new SpyItem();
        Vector3 vhit = new Vector3(0.25, 0.5, 0.75);

        item.getHitDepth(vhit, 3);

        assertEquals(0.25, vhit.x);
        assertEquals(0.5, vhit.y);
        assertEquals(0.75, vhit.z);
    }

    @Test
    void deprecatedBridgeMatchesTheHitDepth() {
        SpyItem item = new SpyItem();
        Vector3 vhit = new Vector3(0.25, 0.5, 0.75);

        for (int side = 0; side < 6; side++) {
            assertEquals(item.getHitDepth(vhit, side), TItemMultiPart$class.getHitDepth(item, vhit, side));
        }
    }

    @Test
    void shallowHitTriesTheClickedBlockThenTheNeighbour() {
        SpyItem item = new SpyItem();

        // side 1 projects onto +y with no offset, so hitY 0.3 gives a depth below 1.
        assertFalse(item.onItemUse(null, null, null, 4, 5, 6, 1, 0f, 0.3f, 0f));

        assertEquals(2, item.attempts.size());
        assertEquals("4,5,6", item.attempts.get(0));
        assertEquals("4,6,6", item.attempts.get(1));
    }

    @Test
    void deepHitOnlyTriesTheNeighbour() {
        SpyItem item = new SpyItem();

        // hitY 1.0 gives a depth of exactly 1, which is not below the threshold.
        assertFalse(item.onItemUse(null, null, null, 4, 5, 6, 1, 0f, 1f, 0f));

        assertEquals(1, item.attempts.size());
        assertEquals("4,6,6", item.attempts.get(0));
    }

    @Test
    void bothAttemptsShareOneMutatedBlockCoordInstance() {
        SpyItem item = new SpyItem();

        item.onItemUse(null, null, null, 4, 5, 6, 1, 0f, 0.3f, 0f);

        assertEquals(2, item.positions.size());
        assertSame(item.positions.get(0), item.positions.get(1));
    }

    @Test
    void newPartReceivesTheHitVectorAndSide() {
        SpyItem item = new SpyItem();

        // Depth here is 0.3, so both attempts run and each must see the same side and hit vector.
        item.onItemUse(null, null, null, 0, 0, 0, 3, 0.1f, 0.2f, 0.3f);

        assertEquals(Arrays.asList(3, 3), item.sides);
        assertSame(item.hits.get(0), item.hits.get(1));
        Vector3 vhit = item.hits.get(0);
        assertEquals(0.1, vhit.x, 1e-6);
        assertEquals(0.2, vhit.y, 1e-6);
        assertEquals(0.3, vhit.z, 1e-6);
    }

    /** Mirrors the forwarders Scala emits, so this file compiles against the trait and against the Java interface. */
    private static final class SpyItem extends Item implements TItemMultiPart {

        private final List<String> attempts = new ArrayList<>();
        private final List<BlockCoord> positions = new ArrayList<>();
        private final List<Vector3> hits = new ArrayList<>();
        private final List<Integer> sides = new ArrayList<>();

        @Override
        public double getHitDepth(Vector3 vhit, int side) {
            return TItemMultiPart$class.getHitDepth(this, vhit, side);
        }

        @Override
        public boolean onItemUse(ItemStack item, EntityPlayer player, World world, int x, int y, int z, int side,
                float hitX, float hitY, float hitZ) {
            return TItemMultiPart$class.onItemUse(this, item, player, world, x, y, z, side, hitX, hitY, hitZ);
        }

        @Override
        public TMultiPart newPart(ItemStack item, EntityPlayer player, World world, BlockCoord pos, int side,
                Vector3 vhit) {
            attempts.add(pos.x + "," + pos.y + "," + pos.z);
            positions.add(pos);
            hits.add(vhit);
            sides.add(side);
            return null;
        }
    }
}
