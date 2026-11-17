package codechicken.multipart.compat

import codechicken.lib.data.MCDataInput
import codechicken.lib.vec.Cuboid6
import codechicken.microblock.TMicroOcclusionClient
import codechicken.multipart.TMultiPart

abstract class ReferenceMicroOcclusionClientBase extends TMultiPart {
  var events: java.util.List[String] = new java.util.ArrayList[String]()
  var failure: RuntimeException = null
  var seenPacket: MCDataInput = null
  var seenPart: TMultiPart = null
  var afterSuper: Runnable = null
  private def predecessor(name: String) {
    events.add(name)
    if (failure != null) throw failure
    if (afterSuper != null) afterSuper.run()
  }
  override def onAdded() { predecessor("superAdded") }
  override def onPartChanged(part: TMultiPart) {
    seenPart = part
    predecessor("superChanged")
  }
  override def read(packet: MCDataInput) {
    seenPacket = packet
    predecessor("superRead")
  }
}

/** Frozen Scala lifecycle predecessors and trait forwarders. */
class ReferenceScalaTMicroOcclusionClient extends ReferenceMicroOcclusionClientBase with TMicroOcclusionClient {
  var bounds: Cuboid6 = new Cuboid6(0, 0, 0, 0.25, 1, 1)
  var slot = 4
  var size = 2
  var transparent = false
  var runRecalc = true
  var recalcFailure: RuntimeException = null
  override def getType = "test:micro_occlusion_client"
  override def getBounds = { events.add("bounds"); bounds }
  override def getSlot = slot
  override def getSize = size
  override def getMaterial = 0
  override def isTransparent = transparent
  override def recalcBounds() {
    events.add("recalc")
    if (recalcFailure != null) throw recalcFailure
    if (runRecalc) super.recalcBounds()
  }
}
