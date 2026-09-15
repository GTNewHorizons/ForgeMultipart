package codechicken.microblock

import codechicken.lib.vec.Vector3
import codechicken.multipart.TFacePart

// Retain Scala inheritance metadata and helper bridges for generated and external mixins.
trait FaceMicroblockClient extends CommonMicroblockClient {
  override def render(pos: Vector3, pass: Int) =
    FaceMicroblockTraitLogic.render(this, pos, pass)
}

trait FaceMicroblock extends CommonMicroblock with TFacePart {
  def microClass = FaceMicroClass$.MODULE$

  def getBounds = FaceMicroblockTraitLogic.getBounds(this)

  override def solid(side: Int) = FaceMicroblockTraitLogic.solid(this, side)
}
