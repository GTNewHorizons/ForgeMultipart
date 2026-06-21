package codechicken.microblock

import codechicken.lib.render.uv.IconTransformation
import codechicken.lib.vec.{Cuboid6, Vector3}
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial
import codechicken.microblock.handler.MicroblockProxy
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.Block
import net.minecraft.block.Block.SoundType
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon

/** Stand-in for a material whose name could not be resolved at load time.
  * Renders the vanilla missing texture (magenta/black checkerboard) and behaves
  * inertly. Resolving an unknown name to this placeholder keeps microblocks
  * from silently becoming material id 0 (Blood Rune in GTNH).
  */
object MissingMicroMaterial extends IMicroMaterial {
  val key = "forgemicroblock:missing"

  @SideOnly(Side.CLIENT)
  private var icon: IIcon = _

  @SideOnly(Side.CLIENT)
  override def loadIcons(): Unit = {
    icon = MicroblockProxy.renderBlocks.getIconSafe(null)
  }

  @SideOnly(Side.CLIENT)
  override def getBreakingIcon(side: Int): IIcon = icon

  @SideOnly(Side.CLIENT)
  override def renderMicroFace(
      pos: Vector3,
      pass: Int,
      bounds: Cuboid6
  ): Unit = {
    MaterialRenderHelper
      .start(pos, pass, new IconTransformation(icon))
      .lighting()
      .render()
  }

  override def isTransparent: Boolean = false
  override def getLightValue: Int = 0
  override def getStrength(player: EntityPlayer): Float = 1f
  override def getLocalizedName: String = "Missing Material"
  override def getItem: ItemStack = new ItemStack(Blocks.stone)
  override def getCutterStrength: Int = 0
  override def getSound: SoundType = Block.soundTypeStone
  override def explosionResistance(entity: Entity): Float = 6f
}
