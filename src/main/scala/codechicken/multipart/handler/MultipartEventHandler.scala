package codechicken.multipart.handler

import codechicken.multipart.{BlockMultipart, TileCache, TileMultipart}
import cpw.mods.fml.common.eventhandler.{EventPriority, SubscribeEvent}
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraft.world.ChunkPosition
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.event.world._

import java.util.List
import scala.collection.JavaConverters._

object MultipartEventHandler {
  @SubscribeEvent(priority = EventPriority.HIGHEST)
  def tileEntityLoad(event: ChunkDataEvent.Load) {
    MultipartSaveLoad.loadTiles(
      event.getChunk.worldObj,
      event.getChunk.chunkTileEntityMap
        .asInstanceOf[java.util.Map[ChunkPosition, TileEntity]]
    )
  }

  @SubscribeEvent
  def worldUnLoad(event: WorldEvent.Unload) {
    MultipartSPH.onWorldUnload(event.world)
    if (event.world.isRemote) {
      TileCache.clear()
    } else {
      MultipartSaveLoad.loadingWorld = null
    }
  }

  @SubscribeEvent
  def chunkWatch(event: ChunkWatchEvent.Watch) {
    MultipartSPH.onChunkWatch(event.player, event.chunk)
  }

  @SubscribeEvent
  def chunkUnWatch(event: ChunkWatchEvent.UnWatch) {
    MultipartSPH.onChunkUnWatch(event.player, event.chunk)
  }

  @SubscribeEvent
  @SideOnly(Side.CLIENT)
  def drawBlockHighlight(event: DrawBlockHighlightEvent) {
    if (
      event.target != null && event.target.typeOfHit == MovingObjectType.BLOCK &&
      event.player.worldObj
        .getTileEntity(
          event.target.blockX,
          event.target.blockY,
          event.target.blockZ
        )
        .isInstanceOf[TileMultipart]
    ) {
      if (
        BlockMultipart.drawHighlight(
          event.player.worldObj,
          event.player,
          event.target,
          event.partialTicks
        )
      )
        event.setCanceled(true)
    }
  }

  @SubscribeEvent
  def serverTick(event: TickEvent.ServerTickEvent) {
    if (event.phase == TickEvent.Phase.END)
      MultipartSPH.onTickEnd(
        MinecraftServer.getServer.getConfigurationManager.playerEntityList
          .asInstanceOf[List[EntityPlayerMP]]
          .asScala
      )
  }
}
