package codechicken.multipart.compat

import codechicken.lib.vec.{Cuboid6, Vector3}
import codechicken.microblock.{CommonMicroblockClient, HollowMicroblockClient, Microblock}
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial

abstract class ReferenceHollowClientBase extends Microblock(41) with CommonMicroblockClient {
  var events: java.util.List[String] = new java.util.ArrayList[String]()
  var failSuper = false
  var superMask = 0x12345665
  renderMask = 0x123401
  override def recalcBounds() {
    events.add("super")
    renderMask = superMask
    if (failSuper) throw new IllegalStateException("super")
  }
}

/** Frozen client forwarders expose the original trait linearization and render callback dispatch. */
class ReferenceScalaHollowMicroblockClient extends ReferenceHollowClientBase with HollowMicroblockClient {
  var bounds = new Cuboid6(0.1, 0.2, 0.3, 0.9, 0.8, 0.7)
  var selectedMaterial: IMicroMaterial = null
  var transparent = false
  var slot = 0
  var slotAdvance = 0
  var hollowSize = 8
  var changeMaskOnSize = false
  var rawShape: Byte = 48
  var shapeAdvance = 0
  var runHollow = false
  var afterHollow: Runnable = null
  var draws: java.util.List[Array[Object]] = new java.util.ArrayList[Array[Object]]()
  override def getBounds = { events.add("bounds"); bounds }
  override def getIMaterial = { events.add("material"); selectedMaterial }
  override def isTransparent = { events.add("transparent"); transparent }
  override def getSlot = { events.add("slot"); val result = slot; slot += slotAdvance; result }
  override def getHollowSize = {
    events.add("size")
    if (changeMaskOnSize) renderMask = 0xdeadbeef
    hollowSize
  }
  override def shape = { events.add("shape"); val result = rawShape; rawShape = (rawShape + shapeAdvance).toByte; result }
  override def x = { events.add("x"); 2 }
  override def y = { events.add("y"); -3 }
  override def z = { events.add("z"); 4 }
  override def renderHollow(pos: Vector3, pass: Int, c: Cuboid6, mask: Int, face: Boolean,
      f: (Vector3, IMicroMaterial, Int, Cuboid6, Int) => Unit) {
    events.add("hollow")
    draws.add(Array[Object](pos, Int.box(pass), c, Int.box(mask), Boolean.box(face), f))
    if (afterHollow != null) afterHollow.run()
    if (runHollow) super.renderHollow(pos, pass, c, mask, face, f)
  }
}
