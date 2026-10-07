package codechicken.microblock

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MovingObjectPosition
import net.minecraft.client.renderer.RenderBlocks
import codechicken.multipart.{TFacePart, TMultiPart, TNormalOcclusion}
import codechicken.lib.vec.{Cuboid6, Vector3}
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial

trait HollowMicroblockClient
    extends HollowMicroblock
    with CommonMicroblockClient {
  renderMask |= 8 << 8

  override def recalcBounds() {
    super.recalcBounds()
    HollowMicroblockClientLogic.updateRenderMask(this)
  }

  override def drawBreaking(renderBlocks: RenderBlocks) =
    HollowMicroblockClientLogic.drawBreaking(this, renderBlocks)

  override def render(pos: Vector3, pass: Int) =
    HollowMicroblockClientLogic.render(this, pos, pass)

  def renderHollow(
      pos: Vector3,
      pass: Int,
      c: Cuboid6,
      sideMask: Int,
      face: Boolean,
      f: (Vector3, IMicroMaterial, Int, Cuboid6, Int) => Unit
  ) = HollowMicroblockClientLogic.renderHollow(
    this,
    pos,
    pass,
    c,
    sideMask,
    face,
    f
  )

  override def drawHighlight(
      hit: MovingObjectPosition,
      player: EntityPlayer,
      frame: Float
  ): Boolean =
    HollowMicroblockClientLogic.drawHighlight(this, hit, player, frame)
}

trait HollowMicroblock
    extends CommonMicroblock
    with TFacePart
    with TNormalOcclusion {
  def microClass = HollowMicroClass$.MODULE$

  def getBounds: Cuboid6 = HollowMicroblockTraitLogic.getBounds(this)

  // Scala retains the synthetic super accessor and its trait linearization.
  override def occlusionTest(npart: TMultiPart): Boolean =
    HollowMicroblockTraitLogic.normalOcclusionTest(this, npart) && super
      .occlusionTest(npart)

  override def getPartialOcclusionBoxes =
    HollowMicroblockTraitLogic.getPartialOcclusionBoxes(this)

  def getHollowSize = HollowMicroblockTraitLogic.getHollowSize(this)

  def getOcclusionBoxes = HollowMicroblockTraitLogic.getOcclusionBoxes(this)

  override def getCollisionBoxes =
    HollowMicroblockTraitLogic.getCollisionBoxes(this)

  override def getSubParts = HollowMicroblockTraitLogic.getSubParts(this)

  override def allowCompleteOcclusion = true

  override def solid(side: Int) = false

  override def redstoneConductionMap = 0x10
}
