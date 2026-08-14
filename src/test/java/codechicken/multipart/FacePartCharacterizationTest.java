package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FacePartCharacterizationTest {

    @Test
    void defaultsAreSolidOnEverySideAndConductNothing() {
        ForwardingPart part = new ForwardingPart();

        for (int side = 0; side < 6; side++) {
            assertTrue(part.solid(side));
        }
        assertTrue(part.solid(-1));
        assertTrue(part.solid(99));
        assertEquals(0, part.redstoneConductionMap());
    }

    @Test
    void deprecatedBridgeMatchesTheDefaults() {
        ForwardingPart part = new ForwardingPart();

        for (int side = 0; side < 6; side++) {
            assertTrue(TFacePart$class.solid(part, side));
        }
        assertEquals(0, TFacePart$class.redstoneConductionMap(part));
    }

    /** The bridge holds the trait defaults rather than dispatching, so an override does not change what it returns. */
    @Test
    void deprecatedBridgeReturnsTheTraitDefaultsEvenWhenOverridden() {
        OverridingPart part = new OverridingPart();

        assertFalse(part.solid(0));
        assertEquals(0x10, part.redstoneConductionMap());
        assertTrue(TFacePart$class.solid(part, 0));
        assertEquals(0, TFacePart$class.redstoneConductionMap(part));
    }

    @Test
    void facePartIsASlottedPart() {
        ForwardingPart part = new ForwardingPart();

        assertTrue(part instanceof TSlottedPart);
        assertEquals(0x3f, part.getSlotMask());
    }

    /** Mirrors the forwarders Scala emits for a class mixing in the trait without overriding either member. */
    private static class ForwardingPart extends TMultiPart implements TFacePart {

        @Override
        public String getType() {
            return "test:face";
        }

        @Override
        public int getSlotMask() {
            return 0x3f;
        }

        @Override
        public boolean solid(int side) {
            return TFacePart$class.solid(this, side);
        }

        @Override
        public int redstoneConductionMap() {
            return TFacePart$class.redstoneConductionMap(this);
        }
    }

    private static final class OverridingPart extends ForwardingPart {

        @Override
        public boolean solid(int side) {
            return false;
        }

        @Override
        public int redstoneConductionMap() {
            return 0x10;
        }
    }
}
