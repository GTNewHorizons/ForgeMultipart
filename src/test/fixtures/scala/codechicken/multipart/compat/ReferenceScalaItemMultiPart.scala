package codechicken.multipart.compat

import codechicken.lib.vec.{BlockCoord, Vector3}
import codechicken.multipart.{TItemMultiPart, TMultiPart}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.world.World

/** Mixes in TItemMultiPart without overriding anything it supplies, so the compiled forwarders call
  * TItemMultiPart$class.getHitDepth, TItemMultiPart$class.onItemUse and TItemMultiPart$class.$init$.
  *
  * newPart records each attempted position and returns null, which is the short-circuit that keeps placement from
  * touching the world.
  */
class ReferenceScalaItemMultiPart extends Item with TItemMultiPart {
  var attempts = ""

  def newPart(
      item: ItemStack,
      player: EntityPlayer,
      world: World,
      pos: BlockCoord,
      side: Int,
      vhit: Vector3
  ): TMultiPart = {
    attempts += s"${pos.x},${pos.y},${pos.z};"
    null
  }
}
