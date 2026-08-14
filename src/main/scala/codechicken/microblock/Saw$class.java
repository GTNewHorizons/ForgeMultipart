package codechicken.microblock;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Binary bridge for Scala implementations compiled against the original trait. Their forwarders call this static
 * directly, so the body cannot delegate back to the instance method without recursing.
 */
@Deprecated
public abstract class Saw$class {

    private Saw$class() {}

    public static int getMaxCuttingStrength(Saw saw) {
        return saw.getCuttingStrength(new ItemStack((Item) saw));
    }

    public static void $init$(Saw saw) {}
}
