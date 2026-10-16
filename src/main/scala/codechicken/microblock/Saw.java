package codechicken.microblock;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Interface for items that are 'saws'.
 * <p>
 * Item does not declare getMaxCuttingStrength, so it can safely be a default method. Implementors must extend
 * {@link Item}; the default casts to it in order to build a stack of itself, exactly as the reference did.
 */
public interface Saw {

    /** The maximum harvest level that some version of this saw is capable of cutting. */
    default int getMaxCuttingStrength() {
        return getCuttingStrength(new ItemStack((Item) this));
    }

    /** The harvest level this saw is capable of cutting. */
    int getCuttingStrength(ItemStack item);
}
