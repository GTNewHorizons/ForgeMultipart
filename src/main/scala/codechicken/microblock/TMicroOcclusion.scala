package codechicken.microblock

import codechicken.multipart.TMultiPart
import codechicken.lib.vec.Cuboid6
import codechicken.lib.data.MCDataInput

trait JMicroShrinkRender {
  def getPriorityClass: Int
  def getSlot: Int
  def getSize: Int
  def isTransparent: Boolean
  def getBounds: Cuboid6
}

trait TMicroOcclusionClient extends TMicroOcclusion with JMicroShrinkRender {
  var renderBounds: Cuboid6 = _
  var renderMask: Int = _

  override def onPartChanged(part: TMultiPart) {
    super.onPartChanged(part)
    recalcBounds()
  }

  override def onAdded() {
    super.onAdded()
    recalcBounds()
  }

  override def read(packet: MCDataInput) {
    super.read(packet)
    recalcBounds()
  }

  def recalcBounds(): Unit =
    TMicroOcclusionClientLogic.recalcBounds(this)

  override def getPriorityClass = 0
}

trait TMicroOcclusion extends TMultiPart {
  def getSlot: Int
  def getSize: Int
  def getMaterial: Int
  def getBounds: Cuboid6

  // Scala retains the synthetic super accessor and trait linearization.
  override def occlusionTest(npart: TMultiPart): Boolean =
    super
      .occlusionTest(npart) && TMicroOcclusionLogic.occlusionTest(this, npart)

  def edgeCornerOcclusionTest(
      edge: TMicroOcclusion,
      corner: TMicroOcclusion
  ): Boolean = TMicroOcclusionLogic.edgeCornerOcclusionTest(edge, corner)
}
