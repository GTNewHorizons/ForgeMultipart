package codechicken.multipart.compat

import codechicken.microblock.ItemMicroPart
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial
import net.minecraft.item.ItemStack

/** Compiled against the reference dev jar. Exercises the ItemMicroPart companion methods used by ProjRed. */
class ReferenceScalaItemMicroPartConsumer {
  def createById(damage: Int, material: Int): ItemStack =
    ItemMicroPart.create(damage, material)

  def createByName(damage: Int, material: String): ItemStack =
    ItemMicroPart.create(damage, material)

  def material(stack: ItemStack): IMicroMaterial =
    ItemMicroPart.getMaterial(stack)

  def materialId(stack: ItemStack): Int =
    ItemMicroPart.getMaterialID(stack)
}
