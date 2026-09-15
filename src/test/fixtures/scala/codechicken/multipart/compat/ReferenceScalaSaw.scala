package codechicken.multipart.compat

import codechicken.microblock.Saw
import net.minecraft.item.{Item, ItemStack}

/** Mixes in Saw without overriding getMaxCuttingStrength, so the compiled forwarder calls
  * Saw$class.getMaxCuttingStrength and the constructor calls Saw$class.$init$.
  *
  * getCuttingStrength reports whether the stack it was handed actually wraps this item, so the test can prove the
  * bridge built the stack rather than only that it linked.
  */
class ReferenceScalaSaw extends Item with Saw {
  def getCuttingStrength(item: ItemStack): Int =
    if (item != null && (item.getItem eq this)) 7 else -1
}
