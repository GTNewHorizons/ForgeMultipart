package codechicken.multipart.compat

import codechicken.lib.vec.Cuboid6
import codechicken.multipart.{TIconHitEffects, TMultiPart}
import net.minecraft.util.IIcon

/** Mixes in TIconHitEffects without overriding anything it or JIconHitEffects supplies, so the compiled forwarders
  * call TIconHitEffects$class.addHitEffects, TIconHitEffects$class.addDestroyEffects, TIconHitEffects$class.$init$,
  * JIconHitEffects$class.getBreakingIcon and JIconHitEffects$class.$init$.
  */
class ReferenceScalaIconHitEffects extends TMultiPart with TIconHitEffects {
  var lastSide = -1

  def getType = "test:reference-icon-hit"

  def getBounds = new Cuboid6(0, 0, 0, 1, 1, 1)

  def getBrokenIcon(side: Int): IIcon = {
    lastSide = side
    null
  }
}
