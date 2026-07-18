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

    val iter = cube.cubeTileEntityMap.entrySet().iterator()

    while (iter.hasNext) {
      val e = iter.next();

      val converted = MultipartSaveLoad.convertTileForCube(world, e.getValue)
      if (converted != e.getValue) {
        if (converted != null) e.setValue(converted)
        else cube.cubeTileEntityMap.remove(e.getKey)
      }
    }
  }
}
