package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.util.IIcon;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Cuboid6;

/**
 * The particle calls themselves need a world and an EffectRenderer, so they cannot run headless. What is asserted here
 * is the part of the contract that is reachable: icon selection, the order and range of the icons requested, and
 * whether the part's own bounds are consulted. Actual particle appearance stays on the manual checklist.
 */
class IconHitEffectsCharacterizationTest {

    @Test
    void breakingIconFallsBackToTheBrokenIconForThatSide() {
        SpyPart part = new SpyPart();

        assertNull(part.getBreakingIcon("ignored-subpart", 3));
        assertEquals(1, part.brokenIconSides.size());
        assertEquals(3, part.brokenIconSides.get(0));
    }

    @Test
    void deprecatedBridgeMatchesTheBreakingIconFallback() {
        SpyPart part = new SpyPart();

        assertNull(JIconHitEffects$class.getBreakingIcon(part, null, 5));
        assertEquals(1, part.brokenIconSides.size());
        assertEquals(5, part.brokenIconSides.get(0));
    }

    @Test
    void destroyEffectsRequestAllSixSidesInOrderAndScaleToTheBounds() {
        SpyPart part = new SpyPart();

        // The tile is null, so this reaches part.tile after collecting the icons and bounds.
        assertThrows(NullPointerException.class, () -> IconHitEffects.addDestroyEffects(part, null));

        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), part.brokenIconSides);
        assertEquals(1, part.boundsRequested);
    }

    @Test
    void destroyEffectsWithoutDensityScalingIgnoreTheBounds() {
        SpyPart part = new SpyPart();

        assertThrows(NullPointerException.class, () -> IconHitEffects.addDestroyEffects(part, null, false));

        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), part.brokenIconSides);
        assertEquals(0, part.boundsRequested);
    }

    @Test
    void iconHitEffectsPartIsAMultiPart() {
        SpyPart part = new SpyPart();

        assertTrue(part instanceof JIconHitEffects);
        assertTrue(part instanceof TMultiPart);
    }

    /** Mirrors the forwarder Scala emits for a class that does not override getBreakingIcon. */
    private static final class SpyPart extends TMultiPart implements JIconHitEffects {

        private final List<Integer> brokenIconSides = new ArrayList<>();
        private int boundsRequested;

        @Override
        public String getType() {
            return "test:icon-hit";
        }

        @Override
        public Cuboid6 getBounds() {
            boundsRequested++;
            return new Cuboid6(0, 0, 0, 1, 1, 1);
        }

        @Override
        public IIcon getBrokenIcon(int side) {
            brokenIconSides.add(side);
            return null;
        }

        @Override
        public IIcon getBreakingIcon(Object subPart, int side) {
            return JIconHitEffects$class.getBreakingIcon(this, subPart, side);
        }
    }
}
