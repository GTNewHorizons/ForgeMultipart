package codechicken.microblock

import net.minecraft.block.Block
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial
import net.minecraft.util.IIcon
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.minecraft.item.ItemStack
import codechicken.lib.vec.{Cuboid6, Vector3}
import net.minecraft.entity.player.EntityPlayer
import codechicken.lib.render.{
  CCRenderPipeline,
  CCRenderState,
  ColourMultiplier
}
import codechicken.microblock.handler.{MicroblockMod, MicroblockProxy}
import net.minecraft.entity.Entity
import codechicken.lib.render.uv.{MultiIconTransformation, UVTransformation}
import cpw.mods.fml.common.registry.{GameData, GameRegistry}

import java.util.function.Supplier

private class ThreadState {
  var pass = 0
  var builder: CCRenderPipeline#PipelineBuilder = _
}

object MaterialRenderHelper {
  private val threadState =
    ThreadLocal.withInitial[ThreadState](new Supplier[ThreadState]() {
      override def get(): ThreadState = new ThreadState()
    })
  def pass = threadState.get().pass
  def pass_=(pass: Int) = threadState.get().pass = pass
  def builder = threadState.get().builder
  def builder_=(builder: CCRenderPipeline#PipelineBuilder) =
    threadState.get().builder = builder

  def start(pos: Vector3, pass: Int, uvt: UVTransformation) = {
    this.pass = pass
    builder = CCRenderState.instance().pipeline.builder()
    builder.add(pos.translation()).add(uvt)
    this
  }

  def blockColour(colour: Int) = {
    builder.add(ColourMultiplier.instance(colour))
    this
  }

  def lighting() = {
    if (pass != -1)
      builder.add(CCRenderState.instance().lightMatrix)
    this
  }

  def blockAndMeta(b: Block, meta: Int) = {
    if (MicroblockMod.angelicaCompat != null)
      MicroblockMod.angelicaCompat.setShaderMaterialOverride(b, meta)
    this
  }

  def render(): Unit = {
    builder.render()
    if (MicroblockMod.angelicaCompat != null)
      MicroblockMod.angelicaCompat.resetShaderMaterialOverride()
  }
}

/** Standard micro material class suitable for most blocks.
  */
class BlockMicroMaterial(val block: Block, val meta: Int = 0)
    extends IMicroMaterial {
  val blockKey = Block.blockRegistry.getNameForObject(block)

  @SideOnly(Side.CLIENT)
  var icont: MultiIconTransformation = _

  @SideOnly(Side.CLIENT)
  override def loadIcons() {
    def safeIcon(block: Block, side: Int): IIcon = {
      try {
        return MicroblockProxy.renderBlocks.getIconSafe(
          block.getIcon(side, meta)
        )
      } catch {
        case e: Exception =>
      }
      return MicroblockProxy.renderBlocks.getIconSafe(null)
    }

    val iblock = Block.getBlockFromName(
      blockKey
    ) // reacquire block instance incase a mod replcaed it
    icont = new MultiIconTransformation(
      Array.tabulate(6)(side => safeIcon(iblock, side)): _*
    )
  }

  def renderMicroFace(pos: Vector3, pass: Int, bounds: Cuboid6) {
    MaterialRenderHelper
      .start(pos, pass, icont)
      .blockColour(getColour(pass))
      .lighting()
      .blockAndMeta(block, meta)
      .render()
  }

  def getColour(pass: Int) = {
    if (pass == -1)
      block.getBlockColor << 8 | 0xff
    else {
      val state = CCRenderState.instance
      val pos = state.lightMatrix.pos
      block.colorMultiplier(
        state.lightMatrix.access,
        pos.x,
        pos.y,
        pos.z
      ) << 8 | 0xff
    }
  }

  override def canRenderInPass(pass: Int) = block.canRenderInPass(pass)

  @SideOnly(Side.CLIENT)
  def getBreakingIcon(side: Int) = block.getIcon(side, meta)

  def getItem = new ItemStack(block, 1, meta)

  def getLocalizedName = getItem.getDisplayName

  def getStrength(player: EntityPlayer) = {
    var hardness = 30f
    try {
      hardness = block.getBlockHardness(null, 0, 0, 0)
    } catch {
      case e: Exception =>
    }
    player.getBreakSpeed(block, false, meta % 16, 0, -1, 0) / hardness
  }

  def isTransparent = !block.isOpaqueCube

  def getLightValue = block.getLightValue

  def toolClasses = Seq("axe", "pickaxe", "shovel")

  // meta > 15 will throw ArrayIndexOutOfBoundsException
  def getCutterStrength = block.getHarvestLevel(meta % 16)

  def getSound = block.stepSound

  def explosionResistance(entity: Entity): Float =
    block.getExplosionResistance(entity)
}

/** Utility functions for cleaner registry code
  */
object BlockMicroMaterial {
  def oldKey(block: Block) = block.getUnlocalizedName
  def materialKey(block: Block) = Block.blockRegistry.getNameForObject(block)
  def materialKey(name: String, meta: Int) =
    name + (if (meta > 0) "_" + meta else "")
  def materialKey(block: Block, meta: Int): String =
    materialKey(materialKey(block), meta)

  def createAndRegister(block: Block, meta: Int, name: String) =
    MicroMaterialRegistry.registerMaterial(
      new BlockMicroMaterial(block, meta),
      materialKey(name, meta)
    )

  def createAndRegister(
      block: Block,
      meta: Int,
      name: String,
      oldName: String
  ) {
    MicroMaterialRegistry.remapName(
      materialKey(oldName, meta),
      materialKey(name, meta)
    )
    createAndRegister(block, meta, name)
  }

  def createAndRegister(block: Block, meta: Int = 0): Unit =
    createAndRegister(block, Seq(meta))
  def createAndRegister(block: Block, meta: Seq[Int]): Unit =
    createAndRegister(block, meta, materialKey(block), oldKey(block))
  def createAndRegister(block: Block, meta: Seq[Int], oldName: String): Unit =
    createAndRegister(block, 0, materialKey(block), oldName)

  def createAndRegister(
      block: Block,
      meta: Seq[Int],
      name: String,
      oldName: String
  ): Unit =
    meta.foreach(m => createAndRegister(block, m, name, oldName))
}
