package codechicken.microblock

import codechicken.lib.data.MCDataInput
import codechicken.lib.vec.{Cuboid6, Vector3}
import codechicken.multipart.{
  JPartialOcclusion,
  NormalOcclusionTest,
  TEdgePart,
  TMultiPart,
  TNormalOcclusion
}
import scala.collection.JavaConversions._

trait EdgeMicroblock extends CommonMicroblock with TEdgePart {
  override def setShape(size: Int, slot: Int) = shape_$eq(
    (size << 4 | (slot - 15)).toByte
  )

  def microClass = EdgeMicroClass$.MODULE$

  def getBounds = EdgeMicroClass.aBounds()(shape)

  override def getSlot = getShape + 15
}

trait PostMicroblockClient extends PostMicroblock with MicroblockClient {
  var renderBounds1: Cuboid6 = _
  var renderBounds2: Cuboid6 = _

  override def render(pos: Vector3, pass: Int) {
    val mat = getIMaterial
    if (pass == -1)
      MicroblockRender.renderCuboid(pos, mat, pass, getBounds, 0)
    else {
      MicroblockRender.renderCuboid(pos, mat, pass, renderBounds1, 0)
      if (renderBounds2 != null)
        MicroblockRender.renderCuboid(pos, mat, pass, renderBounds2, 0)
    }
  }

  override def onPartChanged(part: TMultiPart) {
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
    renderBounds1 = getBounds.copy
    renderBounds2 = null

    shrinkFace(getShape << 1)
    shrinkFace(getShape << 1 | 1)

    tile.partList.foreach(p =>
      if (p.isInstanceOf[PostMicroblock] && p != this)
        shrinkPost(p.asInstanceOf[PostMicroblock])
    )
  }

  def shrinkFace(fside: Int) {
    val part = tile.partMap(fside)
    if (part != null && part.getType.equals("mcr_face"))
      MicroOcclusion.shrink(
        renderBounds1,
        part.asInstanceOf[CommonMicroblock].getBounds,
        fside
      )
  }

  def shrinkPost(post: PostMicroblock) {
    if (post == this)
      return

    if (thisShrinks(post)) {
      if (renderBounds2 == null)
        renderBounds2 = getBounds.copy
      MicroOcclusion.shrink(renderBounds1, post.getBounds, getShape << 1 | 1)
      MicroOcclusion.shrink(renderBounds2, post.getBounds, getShape << 1)
    }
  }

  def thisShrinks(other: PostMicroblock): Boolean = {
    if (getSize != other.getSize) return getSize < other.getSize
    if (isTransparent != other.isTransparent) return isTransparent
    return getShape > other.getShape
  }
}

trait PostMicroblock
    extends Microblock
    with JPartialOcclusion
    with TNormalOcclusion {
  def microClass = PostMicroClass$.MODULE$

  def getBounds = PostMicroClass.aBounds()(shape)

  def getOcclusionBoxes = Seq(getBounds)

  def getPartialOcclusionBoxes = getOcclusionBoxes

  override def itemClassID = EdgeMicroClass.getClassId()

  override def occlusionTest(npart: TMultiPart): Boolean = {
    if (npart.isInstanceOf[PostMicroblock])
      return npart.asInstanceOf[PostMicroblock].getShape != getShape

    if (npart.getType.equals("mcr_face"))
      if (npart.asInstanceOf[CommonMicroblock].getSlot >> 1 == getShape)
        return true

    // TNormalOcclusion is a Java interface, so its box test must be applied here rather than by the super chain.
    return NormalOcclusionTest.apply(this, npart) && super.occlusionTest(npart)
  }

  def getResistanceFactor = PostMicroClass.getResistanceFactor()

  override def canPlaceTorchOnTop = getShape == 0
}
