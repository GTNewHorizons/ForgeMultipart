package codechicken.multipart

import java.util.Random

import codechicken.lib.vec.{Cuboid6, Vector3}
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.IBlockAccess
import net.minecraftforge.client.ForgeHooksClient

import scala.collection.JavaConversions._

trait TileMultipartClient extends TileMultipart {
  private var cachedRenderBounds: AxisAlignedBB = null
  private var staticCache: Array[TMultiPart] = null
  private var dynamicCache: Array[TMultiPart] = null

  var hasDynamicParts: Boolean = false

  def updateRenderCache() {
    if (partList != null) {
      val (dynamic, static) =
        partList.partition(p => p.doesTick || p.shouldRenderDynamic)
      val sArr = static.toArray
      val dArr = dynamic.toArray

      var c: Cuboid6 = null
      var i = 0
      val allParts = sArr ++ dArr
      while (i < allParts.length) {
        val b = allParts(i).getRenderBounds
        if (c == null) c = b.copy
        else c.enclose(b)
        i += 1
      }

      if (c == null) c = Cuboid6.full

      c.add(Vector3.fromTileEntity(this))
      cachedRenderBounds = c.toAABB
      staticCache = sArr
      dynamicCache = dArr
      hasDynamicParts = dArr.length > 0
    } else {
      staticCache = Array.empty
      dynamicCache = Array.empty
      hasDynamicParts = false
      cachedRenderBounds = AxisAlignedBB.getBoundingBox(
        xCoord,
        yCoord,
        zCoord,
        xCoord + 1,
        yCoord + 1,
        zCoord + 1
      )
    }
  }

  def renderStatic(
      world: IBlockAccess,
      vec: Vector3,
      renderer: RenderBlocks
  ) = {
    if (staticCache == null)
      updateRenderCache()

    var rendered = false

    def renderPart(part: TMultiPart): Unit = {
      if (part == null)
        return

      part match {
        case isbrh: ISBRHPart =>
          if (
            isbrh.renderWorldBlock(
              world,
              vec.x.toInt,
              vec.y.toInt,
              vec.z.toInt,
              renderer
            )
          ) {
            rendered = true
          }

        case _ =>
          if (
            part.renderStatic(
              vec,
              ForgeHooksClient.getWorldRenderPass
            )
          ) {
            rendered = true
          }
      }
    }

    val statics = staticCache
    if (statics != null) {
      var i = 0
      val len = statics.length

      while (i < len) {
        renderPart(statics(i))
        i += 1
      }
    }

    val dynamics = dynamicCache
    if (dynamics != null) {
      var i = 0
      val len = dynamics.length

      while (i < len) {
        renderPart(dynamics(i))
        i += 1
      }
    }

    rendered
  }

  def renderDynamic(pos: Vector3, frame: Float, pass: Int) {
    if (!hasDynamicParts) return

    val dynamics = dynamicCache
    if (dynamics != null) {
      var i = 0
      val len = dynamics.length
      while (i < len) {
        if (dynamics(i) != null) dynamics(i).renderDynamic(pos, frame, pass)
        i += 1
      }
    }
  }

  def randomDisplayTick(random: Random) {}

  override def shouldRenderInPass(pass: Int) = {
    if (staticCache == null) updateRenderCache()
    hasDynamicParts
  }

  override def getRenderBoundingBox = {
    if (cachedRenderBounds == null) updateRenderCache()
    cachedRenderBounds
  }

  override def markRender(): Unit = {
    super.markRender()
    updateRenderCache()
  }
}
