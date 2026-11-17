package codechicken.microblock.examples;

import net.minecraft.item.Item;

import codechicken.microblock.ItemSaw;
import codechicken.microblock.handler.MicroblockProxy;

/** Compiling example for docs/api/SAW_STRENGTH.md. */
public final class SawStrengthExample {

    private SawStrengthExample() {}

    /** Applies a consumer's remapped harvest level to an existing FMP saw. */
    public static void updateStrength(ItemSaw saw, int harvestLevel) {
        saw.setHarvestLevel(harvestLevel);
    }

    /** Returns the built-in saws without accessing the retained Scala companion. */
    public static Item[] builtInSaws() {
        return new Item[] { MicroblockProxy.sawStone(), MicroblockProxy.sawIron(), MicroblockProxy.sawDiamond() };
    }

    /** Returns whether FMP uses item icons instead of its 3D saw renderer. */
    public static boolean usesSawIcons() {
        return MicroblockProxy.useSawIcons();
    }
}
