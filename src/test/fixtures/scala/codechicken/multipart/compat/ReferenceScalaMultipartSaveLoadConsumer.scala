package codechicken.multipart.compat

import codechicken.multipart.handler.MultipartSaveLoad
import net.minecraft.world.World

/** Compiled against the reference dev jar. Reads MultipartSaveLoad$.MODULE$ and calls the loadingWorld getter/setter
  * that ProjectRed links against.
  */
class ReferenceScalaMultipartSaveLoadConsumer {
  def loadingWorld: World = MultipartSaveLoad.loadingWorld

  def loadingWorld_=(world: World) {
    MultipartSaveLoad.loadingWorld = world
  }
}
