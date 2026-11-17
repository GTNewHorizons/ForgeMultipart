package codechicken.multipart.compat

import codechicken.lib.vec.Cuboid6
import codechicken.microblock.TMicroOcclusion
import codechicken.multipart.TMultiPart

abstract class ReferenceMicroOcclusionBase extends TMultiPart {
  var events: java.util.List[String] = new java.util.ArrayList[String]()
  var label = "self"
  var superResult = true
  var superFailure = false
  override def occlusionTest(other: TMultiPart): Boolean = {
    events.add("super:" + label)
    if (superFailure) throw new IllegalStateException("super")
    superResult
  }
}

/** Frozen Scala trait forwarders with a real superclass and observable virtual calls. */
class ReferenceScalaTMicroOcclusion extends ReferenceMicroOcclusionBase with TMicroOcclusion {
  var slot = 0
  var slotAdvance = 0
  var size = 5
  var material = 0
  var failOn = ""
  var useActualEdge = true
  var edgeResult = true
  var seenEdge: TMicroOcclusion = null
  var seenCorner: TMicroOcclusion = null
  def getType = "test:micro_occlusion"
  def getBounds: Cuboid6 = throw new AssertionError("bounds must not be read")
  private def record(name: String) {
    events.add(name + ":" + label)
    if (failOn == name) throw new IllegalStateException(name)
  }
  def getSlot = { record("slot"); val result = slot; slot += slotAdvance; result }
  def getSize = { record("size"); size }
  def getMaterial = { record("material"); material }
  override def edgeCornerOcclusionTest(edge: TMicroOcclusion, corner: TMicroOcclusion): Boolean = {
    record("edge")
    seenEdge = edge
    seenCorner = corner
    if (useActualEdge) super.edgeCornerOcclusionTest(edge, corner) else edgeResult
  }
}
