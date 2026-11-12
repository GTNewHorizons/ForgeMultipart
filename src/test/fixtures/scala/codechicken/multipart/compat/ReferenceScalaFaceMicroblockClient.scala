package codechicken.multipart.compat

import codechicken.lib.vec.Cuboid6
import codechicken.microblock._
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial

/** Frozen face-client implementor; its forwarder invokes FaceMicroblockClient$class.render. */
class ReferenceScalaFaceMicroblockClient extends Microblock(37) with FaceMicroblockClient {
  val events = new java.util.ArrayList[String]()
  var selectedMaterial: IMicroMaterial = _
  var bounds = new Cuboid6(0, 0, 0, 1, 0.25, 1)
  var transparent = false
  var selectedSlot = 0
  var slotAdvance = 0

  def microClass: CommonMicroClass = null

  override def getIMaterial = {
    events.add("material")
    selectedMaterial
  }

  override def isTransparent = {
    events.add("transparent")
    transparent
  }

  def getBounds = {
    events.add("bounds")
    bounds
  }

  override def getSlot = {
    events.add("slot:" + selectedSlot)
    val slot = selectedSlot
    selectedSlot += slotAdvance
    slot
  }
}
