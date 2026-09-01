package codechicken.multipart

import codechicken.lib.render.CCRenderState
import codechicken.lib.vec.Vector3
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
import cpw.mods.fml.client.registry.RenderingRegistry
import net.minecraft.block.Block
import net.minecraft.client.renderer.{Tessellator, RenderBlocks}
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.IBlockAccess
import net.minecraft.client.Minecraft
import net.minecraftforge.client.ForgeHooksClient
import net.minecraftforge.client.MinecraftForgeClient
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import codechicken.lib.raytracer.ExtendedMOP
import codechicken.lib.lighting.LightMatrix
import com.gtnewhorizons.angelica.api.ThreadSafeISBRH

/** Internal class for rendering callbacks. Should be moved to the handler
  * package
  */
@SideOnly(Side.CLIENT)
object MultipartRenderer
    extends TileEntitySpecialRenderer
    with ISimpleBlockRenderingHandler {
  TileMultipart.renderID = RenderingRegistry.getNextAvailableRenderId

  override def renderTileEntityAt(
      t: TileEntity,
      x: Double,
      y: Double,
      z: Double,
      f: Float
  ) {
    val tmpart = t.asInstanceOf[TileMultipartClient]
    if (tmpart.partList.isEmpty | !tmpart.hasDynamicParts)
      return

    val state = CCRenderState.instance
    state.resetInstance()
    state.pullLightmapInstance()
    state.useNormals = true

    val pos = new Vector3(x, y, z)
    tmpart.renderDynamic(pos, f, MinecraftForgeClient.getRenderPass)
  }

  override def getRenderId = TileMultipart.renderID

  override def renderWorldBlock(
      world: IBlockAccess,
      x: Int,
      y: Int,
      z: Int,
      block: Block,
      modelId: Int,
      renderer: RenderBlocks
  ): Boolean = {
    val t = world.getTileEntity(x, y, z)
    if (!t.isInstanceOf[TileMultipartClient])
      return false

    val tmpart = t.asInstanceOf[TileMultipartClient]
    if (tmpart.partList.isEmpty)
      return false

    if (renderer.hasOverrideBlockTexture) {
      val hit = Minecraft.getMinecraft.objectMouseOver
      if (
        hit != null && hit.blockX == x && hit.blockY == y && hit.blockZ == z && ExtendedMOP
          .getData(hit)
          .isInstanceOf[(_, _)]
      ) {
        val hitInfo: (Int, _) = ExtendedMOP.getData(hit)
        if (
          hitInfo._1.isInstanceOf[
            Int
          ] && hitInfo._1 >= 0 && hitInfo._1 < tmpart.partList.size
        )
          {
            val part = tmpart.partList(hitInfo._1)

            part match {
              case isbrhPart: ISBRHPart =>
                isbrhPart.renderWorldBlock(world, x, y, z, renderer)

              case _ =>
                part.drawBreaking(renderer)
            }
          }

      }
      return false
    }

    val state = CCRenderState.instance
    state.resetInstance()
    state.lightMatrix.locate(world, x, y, z)
    val b = tmpart.renderStatic(world, new Vector3(x, y, z), renderer)
    state.lightMatrix.access = null
    b
  }

  override def renderInventoryBlock(
      block: Block,
      meta: Int,
      modelId: Int,
      renderer: RenderBlocks
  ) {}

  def shouldRender3DInInventory(modelId: Int) = false
}
