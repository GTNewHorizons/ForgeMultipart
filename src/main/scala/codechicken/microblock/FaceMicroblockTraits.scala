package codechicken.microblock

import codechicken.lib.vec.{Cuboid6, Vector3}
import codechicken.multipart.TFacePart

trait FaceMicroblockClient extends CommonMicroblockClient {
  override def render(pos: Vector3, pass: Int) {
    if (pass < 0)
      MicroblockRender.renderCuboid(pos, getIMaterial, pass, getBounds, 0)
    else if (isTransparent)
      MicroblockRender.renderCuboid(
        pos,
        getIMaterial,
        pass,
        renderBounds,
        renderMask
      )
    else {
      val mat = getIMaterial
      MicroblockRender.renderCuboid(
        pos,
        mat,
        pass,
        renderBounds,
        renderMask | 1 << getSlot
      )
      MicroblockRender.renderCuboid(
        pos,
        mat,
        pass,
        Cuboid6.full,
        ~(1 << getSlot)
      )
    }
  }
}

trait FaceMicroblock extends CommonMicroblock with TFacePart {
  def microClass = FaceMicroClass$.MODULE$

  def getBounds = FaceMicroClass.aBounds()(shape)

  override def solid(side: Int) = getIMaterial.isSolid
}
