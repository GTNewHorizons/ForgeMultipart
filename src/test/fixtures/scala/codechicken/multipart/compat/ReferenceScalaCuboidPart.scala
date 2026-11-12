package codechicken.multipart.compat

import codechicken.lib.vec.Cuboid6
import codechicken.multipart.TCuboidPart

/** Mixes in TCuboidPart without overriding anything it supplies, so the compiled forwarders call
  * TCuboidPart$class.$init$, getSubParts, getCollisionBoxes and drawBreaking directly.
  */
class ReferenceScalaCuboidPart extends TCuboidPart {
  def getType = "test:reference-cuboid"

  def getBounds = new Cuboid6(0, 0, 0, 0.5, 0.5, 0.5)
}
