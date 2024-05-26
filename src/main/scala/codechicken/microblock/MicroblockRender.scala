package codechicken.microblock

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MovingObjectPosition
import codechicken.lib.vec.{Cuboid6, Vector3}
import org.lwjgl.opengl.GL11._
import codechicken.lib.render.{CCRenderState, TextureUtils}
import codechicken.lib.render.BlockRenderer.BlockFace
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial
import java.util.function.Supplier

object MicroblockRender {
  def renderHighlight(
      player: EntityPlayer,
      hit: MovingObjectPosition,
      mcrClass: CommonMicroClass,
      size: Int,
      material: Int
  ) {
    mcrClass.placementProperties.placementGrid
      .render(new Vector3(hit.hitVec), hit.sideHit)

    val placement = MicroblockPlacement(
      player,
      hit,
      size,
      material,
      !player.capabilities.isCreativeMode,
      mcrClass.placementProperties
    )
    if (placement == null)
      return
    val pos = placement.pos
    val part = placement.part.asInstanceOf[MicroblockClient]
    val state = CCRenderState.instance

    glPushMatrix()
    glTranslated(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
    glScaled(1.002, 1.002, 1.002)
    glTranslated(-0.5, -0.5, -0.5)

    glEnable(GL_BLEND)
    glDepthMask(false)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    // TODO: update to use a thread local copy
    TextureUtils.bindAtlas(0)
    state.reset()
    state.alphaOverride = 80
    state.useNormals = true
    state.startDrawing()
    part.render(Vector3.zero, -1)
    state.draw()

    glDisable(GL_BLEND)
    glDepthMask(true)
    glPopMatrix()
  }

  val face = ThreadLocal.withInitial[BlockFace](new Supplier[BlockFace]() {
    override def get(): BlockFace = new BlockFace()
  })
  def renderCuboid(
      pos: Vector3,
      mat: IMicroMaterial,
      pass: Int,
      c: Cuboid6,
      faces: Int
  ) {
    val localFace = face.get()
    CCRenderState.instance().setModel(localFace)
    for (s <- 0 until 6 if (faces & 1 << s) == 0) {
      localFace.loadCuboidFace(c, s)
      mat.renderMicroFace(pos, pass, c)
    }
  }
}
