package codechicken.multipart.handler

import com.cardinalstar.cubicchunks.api.event.CubeEvent
import cpw.mods.fml.common.eventhandler.{EventPriority, SubscribeEvent}
import net.minecraftforge.common.MinecraftForge
import scala.collection.JavaConversions._

object CubicChunksCompat {
  def init(): Unit = MinecraftForge.EVENT_BUS.register(this)

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  def onTileEntitiesLoad(event: CubeEvent.DataLoad): Unit = {
    val cube = event.cube
    val world = event.world

    MultipartSaveLoad.loadTiles(world, cube.cubeTileEntityMap)
  }
}
