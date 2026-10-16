package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EdgePartCharacterizationTest {

    @Test
    void edgePartsDoNotConductRedstoneByDefault() {
        assertFalse(new ForwardingPart().conductsRedstone());
    }

    /** The bridge holds the trait default rather than dispatching, so an override does not change what it returns. */
    @Test
    void deprecatedBridgeReturnsTheTraitDefaultEvenWhenOverridden() {
        assertFalse(TEdgePart$class.conductsRedstone(new ForwardingPart()));
        assertTrue(new ConductingPart().conductsRedstone());
        assertFalse(TEdgePart$class.conductsRedstone(new ConductingPart()));
    }

    @Test
    void edgePartIsASlottedPart() {
        ForwardingPart part = new ForwardingPart();

        assertTrue(part instanceof TSlottedPart);
        assertEquals(1 << 15, part.getSlotMask());
    }

    /** Mirrors the forwarder Scala emits for a class mixing in the trait without overriding it. */
    private static class ForwardingPart extends TMultiPart implements TEdgePart {

        @Override
        public String getType() {
            return "test:edge";
        }

        @Override
        public int getSlotMask() {
            return 1 << 15;
        }

        @Override
        public boolean conductsRedstone() {
            return TEdgePart$class.conductsRedstone(this);
        }
    }

    private static final class ConductingPart extends ForwardingPart {

        @Override
        public boolean conductsRedstone() {
            return true;
        }
    }
}
