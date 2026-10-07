package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

class SawCharacterizationTest {

    @Test
    void maxCuttingStrengthAsksTheSawAboutAStackOfItself() {
        SpySaw saw = new SpySaw(5);

        assertEquals(5, saw.getMaxCuttingStrength());
        assertEquals(1, saw.queried.size());
        ItemStack queried = saw.queried.get(0);
        assertNotNull(queried);
        assertSame(saw, queried.getItem());
        assertEquals(1, queried.stackSize);
    }

    @Test
    void deprecatedBridgeBuildsTheSameStack() {
        SpySaw saw = new SpySaw(3);

        assertEquals(3, Saw$class.getMaxCuttingStrength(saw));
        assertEquals(1, saw.queried.size());
        assertSame(saw, saw.queried.get(0).getItem());
    }

    /**
     * Unlike the constant-returning bridges, this one dispatches getCuttingStrength through the interface, so a
     * subclass override is honoured even when the bridge is called directly.
     */
    @Test
    void deprecatedBridgeHonoursACuttingStrengthOverride() {
        SpySaw saw = new StrongerSaw();

        assertEquals(9, Saw$class.getMaxCuttingStrength(saw));
    }

    private static class SpySaw extends Item implements Saw {

        private final List<ItemStack> queried = new ArrayList<>();
        private final int strength;

        private SpySaw(int strength) {
            this.strength = strength;
        }

        @Override
        public int getCuttingStrength(ItemStack item) {
            queried.add(item);
            return strength;
        }

        @Override
        public int getMaxCuttingStrength() {
            return Saw$class.getMaxCuttingStrength(this);
        }
    }

    private static final class StrongerSaw extends SpySaw {

        private StrongerSaw() {
            super(1);
        }

        @Override
        public int getCuttingStrength(ItemStack item) {
            super.getCuttingStrength(item);
            return 9;
        }
    }
}
