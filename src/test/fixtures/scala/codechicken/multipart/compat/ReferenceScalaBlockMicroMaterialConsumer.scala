package codechicken.multipart.compat

import codechicken.microblock.BlockMicroMaterial
import codechicken.microblock.MaterialRenderHelper

/** Compiled against the reference dev jar. Calls the BlockMicroMaterial and MaterialRenderHelper companion methods
  * used by downstream Scala code.
  */
class ReferenceScalaBlockMicroMaterialConsumer {
  def materialKey(name: String, meta: Int): String = BlockMicroMaterial.materialKey(name, meta)

  def constructorDefault: Int = BlockMicroMaterial.$lessinit$greater$default$2

  def registrationDefault: Int = BlockMicroMaterial.createAndRegister$default$2

  def helperPass(value: Int): Int = {
    MaterialRenderHelper.pass = value
    MaterialRenderHelper.pass
  }
}
