package codechicken.multipart.compat

import codechicken.lib.data.MCDataInput
import codechicken.lib.vec.Cuboid6
import codechicken.microblock.{Microblock, PostMicroblock, PostMicroblockClient}
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial
import codechicken.multipart.TMultiPart

abstract class ReferencePostClientBase extends Microblock(41) {
  var events: java.util.List[String] = new java.util.ArrayList[String]()
  var failSuper = false
  var seenPacket: MCDataInput = null
  override def onAdded() { events.add("superAdded"); if (failSuper) throw new IllegalStateException("super") }
  override def read(packet: MCDataInput) {
    events.add("superRead")
    if (failSuper) throw new IllegalStateException("super")
    seenPacket = packet
  }
  override def onPartChanged(part: TMultiPart) { events.add("superChanged") }
}

/** Frozen client forwarders with observable virtual dispatch, equality and lifecycle predecessors. */
class ReferenceScalaPostMicroblockClient extends ReferencePostClientBase with PostMicroblockClient {
  var label = "self"
  var selectedMaterial: IMicroMaterial = null
  var bounds = new Cuboid6(0.375, 0, 0.375, 0.625, 1, 0.625)
  var selectedSize = 2
  var sizeAdvance = 0
  var selectedShape = 0
  var shapeAdvance = 0
  var transparent = false
  var flipTransparency = false
  var runRecalc = true
  var runShrinks = true
  var forceEquality = false
  var equalityResult = false

  override def getIMaterial = { events.add("material"); selectedMaterial }
  override def getBounds = { events.add("bounds:" + label); bounds }
  override def getSize = {
    events.add("size:" + label)
    val result = selectedSize
    selectedSize += sizeAdvance
    result
  }
  override def getShape = {
    events.add("shape:" + label)
    val result = selectedShape
    selectedShape += shapeAdvance
    result
  }
  override def isTransparent = {
    events.add("transparent:" + label)
    val result = transparent
    if (flipTransparency) transparent = !transparent
    result
  }
  override def tile = { events.add("tile:" + label); super.tile }
  override def recalcBounds() { events.add("recalc"); if (runRecalc) super.recalcBounds() }
  override def shrinkFace(side: Int) { events.add("face:" + side); if (runShrinks) super.shrinkFace(side) }
  override def shrinkPost(post: PostMicroblock) { events.add("post"); if (runShrinks) super.shrinkPost(post) }
  override def equals(other: Any): Boolean = {
    events.add("equals:" + label)
    if (forceEquality) equalityResult else super.equals(other)
  }
  override def hashCode = System.identityHashCode(this)
}
