package codechicken.microblock

import com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator
import net.coderbot.iris.Iris
import net.minecraft.block.Block
import net.minecraft.client.renderer.Tessellator

class AngelicaCompat {
  def setShaderMaterialOverride(block: Block, meta: Int) = {
    try {
      if (Tessellator.instance.isInstanceOf[CapturingTessellator])
        Iris.setShaderMaterialOverride(block, meta)
    } catch {
      case ex: ClassCastException => Unit
    }
  }

  def resetShaderMaterialOverride() = {
    try {
      if (Tessellator.instance.isInstanceOf[CapturingTessellator])
        Iris.resetShaderMaterialOverride()
    } catch {
      case ex: ClassCastException => Unit
    }
  }
}
