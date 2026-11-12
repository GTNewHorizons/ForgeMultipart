package codechicken.multipart.compat

import codechicken.lib.vec.Cuboid6
import codechicken.microblock.{CommonMicroblock, CommonMicroClass, Microblock, PostMicroblock}
import codechicken.multipart.TMultiPart

/** A real superclass predecessor for the frozen PostMicroblock super accessor. */
abstract class ReferencePostMicroblockBase extends Microblock(31) {
  var events: java.util.List[String] = new java.util.ArrayList[String]()
  var label = "self"
  var superResult = true
  var superFailure = false

  override def occlusionTest(part: TMultiPart): Boolean = {
    events.add("super:" + label)
    if (superFailure) throw new IllegalStateException("super failure")
    superResult
  }
}

/** Frozen trait forwarders with observable virtual calls and an actual super chain. */
class ReferenceScalaPostMicroblock extends ReferencePostMicroblockBase with PostMicroblock {
  var selectedShape = 0
  var selectedBounds: Cuboid6 = Cuboid6.full.copy
  var replacementBoxes: java.util.List[Cuboid6] = null

  override def getShape = {
    events.add("shape:" + label)
    selectedShape
  }

  override def getBounds = {
    events.add("bounds:" + label)
    selectedBounds
  }

  override def getType = {
    events.add("type:" + label)
    "mcr_face"
  }

  override def getOcclusionBoxes: java.util.List[Cuboid6] = {
    events.add("boxes:" + label)
    if (replacementBoxes == null) super.getOcclusionBoxes else replacementBoxes
  }
}

class ReferencePostFace extends Microblock(0) with CommonMicroblock {
  var events: java.util.List[String] = new java.util.ArrayList[String]()
  var slot = 0
  def microClass: CommonMicroClass = null
  override def getType = { events.add("type:face"); "mcr_face" }
  override def getSlot = { events.add("slot:face"); slot }
  def getBounds = { events.add("bounds:face"); Cuboid6.full }
}
