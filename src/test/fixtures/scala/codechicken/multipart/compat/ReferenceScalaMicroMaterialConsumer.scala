package codechicken.multipart.compat

import codechicken.microblock.MicroMaterialRegistry
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial

/** Compiled against the reference dev jar. Reads MicroMaterialRegistry$.MODULE$ and calls the instance methods ProjRed
  * links against, plus the Tuple2-array getIdMap that extrautilities links against.
  */
class ReferenceScalaMicroMaterialConsumer {
  def materialAt(id: Int): IMicroMaterial = MicroMaterialRegistry.getMaterial(id)

  def idOf(name: String): Int = MicroMaterialRegistry.materialID(name)

  def nameOf(id: Int): String = MicroMaterialRegistry.materialName(id)

  /** Exercises the raw scala.Tuple2 array descriptor rather than the accessors. */
  def idMapEntry(id: Int): String = {
    val entry = MicroMaterialRegistry.getIdMap.apply(id)
    entry._1 + "=" + (entry._2 eq materialAt(id))
  }
}
