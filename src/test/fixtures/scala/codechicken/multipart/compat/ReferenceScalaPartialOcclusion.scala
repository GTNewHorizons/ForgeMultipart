package codechicken.multipart.compat

import java.util.Collections

import codechicken.lib.vec.Cuboid6
import codechicken.multipart.JPartialOcclusion

final class ReferenceScalaPartialOcclusion extends JPartialOcclusion {
  override def getPartialOcclusionBoxes: java.lang.Iterable[Cuboid6] =
    Collections.emptyList[Cuboid6]()
}
