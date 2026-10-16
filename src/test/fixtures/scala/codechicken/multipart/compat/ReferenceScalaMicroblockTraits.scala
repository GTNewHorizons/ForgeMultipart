package codechicken.multipart.compat

import codechicken.lib.vec.{Cuboid6, Vector3}
import codechicken.microblock._
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial

/** Frozen concrete consumer of the three traits and their generated helper bridges. */
class ReferenceScalaMicroblockTraits extends Microblock(23) with CommonMicroblockClient {
  val events = new java.util.ArrayList[String]()
  var selectedMaterial: IMicroMaterial = _
  var selectedClass: CommonMicroClass = _
  var bounds = new Cuboid6(0, 0, 0, 1, 0.5, 1)
  var overrideSlot = false
  var selectedSlot = 0
  var commonRender = false
  var renderedPosition: Vector3 = _

  override def getIMaterial = {
    events.add("material")
    selectedMaterial
  }

  def microClass = selectedClass

  def getBounds = {
    events.add("bounds")
    bounds
  }

  override def getSlot = if (overrideSlot) selectedSlot else super.getSlot

  override def render(pos: Vector3, pass: Int) {
    events.add("render:" + pass)
    renderedPosition = pos
    if (commonRender) super.render(pos, pass)
  }
}
