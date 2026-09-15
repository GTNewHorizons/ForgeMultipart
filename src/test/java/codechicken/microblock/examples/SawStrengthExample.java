package codechicken.microblock.examples;

import codechicken.microblock.ItemSaw;

/** Compiling example for docs/api/SAW_STRENGTH.md. */
public final class SawStrengthExample {

    private SawStrengthExample() {}

    /** Applies a consumer's remapped harvest level to an existing FMP saw. */
    public static void updateStrength(ItemSaw saw, int harvestLevel) {
        saw.setHarvestLevel(harvestLevel);
    }
}
