package codechicken.microblock

import codechicken.multipart.TMultiPart
import codechicken.lib.vec.Cuboid6
import codechicken.multipart.PartMap._
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

  def recalcBounds() {
    renderBounds = getBounds.copy
    renderMask = MicroOcclusion$.MODULE$.recalcBounds(this, renderBounds)
  }

  override def getPriorityClass = 0
}

trait TMicroOcclusion extends TMultiPart {
  def getSlot: Int
  def getSize: Int
  def getMaterial: Int
  def getBounds: Cuboid6

  override def occlusionTest(npart: TMultiPart): Boolean = {
    if (!super.occlusionTest(npart))
      return false

    if (!npart.isInstanceOf[TMicroOcclusion])
      return true

    val mpart = npart.asInstanceOf[TMicroOcclusion]
    val shape1 = MicroOcclusion$.MODULE$.shapePriority(getSlot)
    val shape2 = MicroOcclusion$.MODULE$.shapePriority(mpart.getSlot)

    if (mpart.getSize + getSize > 8) // intersecting if opposite
      {
        if (shape1 == 2 && shape2 == 2)
          if (mpart.getSlot == (getSlot ^ 1))
            return false

        if (mpart.getMaterial != getMaterial) {
          if (shape1 == 1 && shape2 == 1) {
            val axisMask = (getSlot - 7) ^ (mpart.getSlot - 7)
            if (axisMask == 3 || axisMask == 5 || axisMask == 6)
              return false
          }

          if (shape1 == 0 && shape2 == 1)
            if (!edgeCornerOcclusionTest(this, mpart))
              return false

          if (shape1 == 1 && shape2 == 0)
            if (!edgeCornerOcclusionTest(mpart, this))
              return false

          if (shape1 == 0 && shape2 == 0) {
            val e1 = getSlot - 15
            val e2 = mpart.getSlot - 15
            if ((e1 & 0xc) == (e2 & 0xc) && ((e1 & 3) ^ (e2 & 3)) == 3)
              return false
          }
        }
      }

    return true
  }

  def edgeCornerOcclusionTest(
      edge: TMicroOcclusion,
      corner: TMicroOcclusion
  ): Boolean = {
    ((corner.getSlot - 7) & edgeAxisMask(edge.getSlot - 15)) == unpackEdgeBits(
      edge.getSlot - 15
    )
  }
}
