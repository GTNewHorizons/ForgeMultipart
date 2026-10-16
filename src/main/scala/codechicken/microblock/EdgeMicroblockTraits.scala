package codechicken.microblock

import codechicken.lib.data.MCDataInput
import codechicken.lib.vec.{Cuboid6, Vector3}
import codechicken.multipart.{
  JPartialOcclusion,
  TEdgePart,
  TMultiPart,
  TNormalOcclusion
}

// Retain Scala inheritance metadata and helper bridges for generated and external mixins.
trait EdgeMicroblock extends CommonMicroblock with TEdgePart {
  override def setShape(size: Int, slot: Int) =
    EdgeMicroblockTraitLogic.setShape(this, size, slot)

  def microClass = EdgeMicroClass$.MODULE$

  def getBounds = EdgeMicroblockTraitLogic.getBounds(this)

  override def getSlot = EdgeMicroblockTraitLogic.getSlot(this)
}

trait PostMicroblockClient extends PostMicroblock with MicroblockClient {
  var renderBounds1: Cuboid6 = _
  var renderBounds2: Cuboid6 = _

  override def render(pos: Vector3, pass: Int) =
    PostMicroblockClientLogic.render(this, pos, pass)

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

  def recalcBounds() = PostMicroblockClientLogic.recalcBounds(this)

  def shrinkFace(fside: Int) = PostMicroblockClientLogic.shrinkFace(this, fside)

  def shrinkPost(post: PostMicroblock) =
    PostMicroblockClientLogic.shrinkPost(this, post)

  def thisShrinks(other: PostMicroblock): Boolean =
    PostMicroblockClientLogic.thisShrinks(this, other)
}

trait PostMicroblock
    extends Microblock
    with JPartialOcclusion
    with TNormalOcclusion {
  def microClass = PostMicroClass$.MODULE$

  def getBounds = PostMicroblockTraitLogic.getBounds(this)

  def getOcclusionBoxes = PostMicroblockTraitLogic.getOcclusionBoxes(this)

  def getPartialOcclusionBoxes =
    PostMicroblockTraitLogic.getPartialOcclusionBoxes(this)

  override def itemClassID = PostMicroblockTraitLogic.itemClassID(this)

  override def occlusionTest(npart: TMultiPart): Boolean = {
    // Java source cannot call the synthetic super accessor; keep only that dispatch here.
    val result = PostMicroblockTraitLogic.occlusionResult(this, npart)
    if (result < 0) super.occlusionTest(npart) else result != 0
  }

  def getResistanceFactor = PostMicroblockTraitLogic.getResistanceFactor(this)

  override def canPlaceTorchOnTop =
    PostMicroblockTraitLogic.canPlaceTorchOnTop(this)
}
