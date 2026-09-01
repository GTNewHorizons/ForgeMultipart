package codechicken.microblock

import codechicken.lib.vec.Vector3
import codechicken.multipart.{
  IconHitEffects,
  JPartialOcclusion,
  TIconHitEffects,
  TSlottedPart
}
import net.minecraft.client.particle.EffectRenderer
import net.minecraft.init.Blocks
import net.minecraft.util.MovingObjectPosition
import scala.collection.JavaConversions._

trait MicroblockClient
    extends Microblock
    with TIconHitEffects
    with IMicroMaterialRender {
  def getBrokenIcon(side: Int) = getIMaterial match {
    case null => Blocks.stone.getIcon(0, 0)
    case mat  => mat.getBreakingIcon(side)
  }

  // TIconHitEffects is a Java interface, so these must be declared explicitly or TMultiPart's empty versions win.
  override def addHitEffects(
      hit: MovingObjectPosition,
      effectRenderer: EffectRenderer
  ) = IconHitEffects.addHitEffects(this, hit, effectRenderer)

  override def addDestroyEffects(effectRenderer: EffectRenderer) =
    IconHitEffects.addDestroyEffects(this, effectRenderer)

  override def renderStatic(pos: Vector3, pass: Int) = {
    if (getIMaterial.canRenderInPass(pass)) {
      render(pos, pass)
      true
    } else
      false
  }

  def render(pos: Vector3, pass: Int)

  override def getRenderBounds = getBounds
}

trait CommonMicroblockClient
    extends CommonMicroblock
    with MicroblockClient
    with TMicroOcclusionClient {
  def render(pos: Vector3, pass: Int) {
    if (pass < 0)
      MicroblockRender.renderCuboid(pos, getIMaterial, pass, getBounds, 0)
    else
      MicroblockRender.renderCuboid(
        pos,
        getIMaterial,
        pass,
        renderBounds,
        renderMask
      )
  }
}

trait CommonMicroblock
    extends Microblock
    with JPartialOcclusion
    with TMicroOcclusion
    with TSlottedPart {
  def microClass: CommonMicroClass

  def getSlot = getShape
  def getSlotMask = 1 << getSlot
  def getPartialOcclusionBoxes = Seq(getBounds)

  override def itemClassID = microClass.getClassId
}
