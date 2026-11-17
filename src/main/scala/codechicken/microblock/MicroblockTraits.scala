package codechicken.microblock

import codechicken.lib.vec.Vector3
import codechicken.multipart.{
  IconHitEffects,
  JPartialOcclusion,
  TIconHitEffects,
  TSlottedPart
}
import net.minecraft.client.particle.EffectRenderer
import net.minecraft.util.MovingObjectPosition

// Keep Scala inheritance metadata and helper bridges while derived and external mixins use Scala registration.
trait MicroblockClient
    extends Microblock
    with TIconHitEffects
    with IMicroMaterialRender {
  def getBrokenIcon(side: Int) = MicroblockTraitLogic.getBrokenIcon(this, side)

  // TIconHitEffects is a Java interface, so these must be declared explicitly or TMultiPart's empty versions win.
  override def addHitEffects(
      hit: MovingObjectPosition,
      effectRenderer: EffectRenderer
  ) = IconHitEffects.addHitEffects(this, hit, effectRenderer)

  override def addDestroyEffects(effectRenderer: EffectRenderer) =
    IconHitEffects.addDestroyEffects(this, effectRenderer)

  override def renderStatic(pos: Vector3, pass: Int) =
    MicroblockTraitLogic.renderStatic(this, pos, pass)

  def render(pos: Vector3, pass: Int)

  override def getRenderBounds = MicroblockTraitLogic.getRenderBounds(this)
}

trait CommonMicroblockClient
    extends CommonMicroblock
    with MicroblockClient
    with TMicroOcclusionClient {
  def render(pos: Vector3, pass: Int) =
    MicroblockTraitLogic.render(this, pos, pass)
}

trait CommonMicroblock
    extends Microblock
    with JPartialOcclusion
    with TMicroOcclusion
    with TSlottedPart {
  def microClass: CommonMicroClass

  def getSlot = MicroblockTraitLogic.getSlot(this)
  def getSlotMask = MicroblockTraitLogic.getSlotMask(this)
  def getPartialOcclusionBoxes =
    MicroblockTraitLogic.getPartialOcclusionBoxes(this)

  override def itemClassID = MicroblockTraitLogic.itemClassID(this)
}
