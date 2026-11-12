package codechicken.multipart.compat

import codechicken.lib.vec.Cuboid6
import codechicken.multipart.{TMultiPart, TNormalOcclusion}

/** Mixes in TNormalOcclusion without overriding occlusionTest, so the compiled class carries the generated
  * codechicken$multipart$TNormalOcclusion$$super$occlusionTest accessor and forwards occlusionTest to
  * TNormalOcclusion$class.
  */
class ReferenceScalaNormalOcclusion extends TMultiPart with TNormalOcclusion {
  def getType = "test:reference-normal-occlusion"

  def getOcclusionBoxes: java.lang.Iterable[Cuboid6] =
    java.util.Collections.singletonList(new Cuboid6(0, 0, 0, 0.5, 1, 1))
}
