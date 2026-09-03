package codechicken.multipart.compat

import codechicken.lib.vec.Cuboid6
import codechicken.microblock.{HollowMicroblock, Microblock}
import codechicken.multipart.{TMultiPart, TileMultipart}

abstract class ReferenceHollowMicroblockBase extends Microblock(0) {
  var events: java.util.List[String] = new java.util.ArrayList[String]()
  var superResult = true
  var superFailure = false
  override def occlusionTest(part: TMultiPart): Boolean = {
    events.add("super")
    if (superFailure) throw new IllegalStateException("super failure")
    superResult
  }
}

/** Frozen forwarders retain the real Scala trait super chain and observable virtual getters. */
class ReferenceScalaHollowMicroblock extends ReferenceHollowMicroblockBase with HollowMicroblock {
  var firstTile: TileMultipart = null
  var secondTile: TileMultipart = null
  var tileReads = 0
  var firstShape: Byte = 48
  var secondShape: Byte = 48
  var shapeReads = 0
  var slot = 0
  var overrideSize = false
  var hollowSize = 8
  var overrideCollision = false
  var collision: java.util.List[Cuboid6] = null
  var normal: java.lang.Iterable[Cuboid6] = java.util.Collections.singletonList(Cuboid6.full)

  override def tile = {
    events.add("tile")
    tileReads += 1
    if (tileReads == 1) firstTile else secondTile
  }
  override def shape = {
    events.add("shape")
    shapeReads += 1
    if (shapeReads == 1) firstShape else secondShape
  }
  override def getSlot = { events.add("slot"); slot }
  override def getHollowSize = {
    events.add("size")
    if (overrideSize) hollowSize else super.getHollowSize
  }
  override def getCollisionBoxes: java.util.List[Cuboid6] = {
    events.add("collision")
    if (overrideCollision) collision else super.getCollisionBoxes
  }
  override def getOcclusionBoxes: java.lang.Iterable[Cuboid6] = {
    events.add("normal:self")
    normal
  }
}
