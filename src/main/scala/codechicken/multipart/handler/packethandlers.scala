package codechicken.multipart.handler

import codechicken.lib.packet.PacketCustom.{
  IHandshakeHandler,
  IClientPacketHandler,
  IServerPacketHandler
}
import codechicken.lib.packet.PacketCustom
import net.minecraft.client.Minecraft
import codechicken.multipart.MultiPartRegistry
import net.minecraft.entity.player.EntityPlayerMP
import codechicken.multipart.ControlKeyModifer
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import java.util.{Map => JMap}
import java.util.Iterator
import net.minecraft.tileentity.TileEntity
import codechicken.multipart.TileMultipart
import net.minecraft.entity.player.EntityPlayer
import scala.collection.mutable.{Map, Set, HashMap, MultiMap}
import codechicken.lib.vec.BlockCoord
import codechicken.lib.data.MCDataOutputWrapper
import java.io.DataOutputStream
import java.io.ByteArrayOutputStream
import net.minecraft.world.ChunkCoordIntPair
import MultipartProxy._
import com.gtnewhorizon.gtnhlib.api.world.WorldContextRegistry
import com.gtnewhorizon.gtnhlib.api.world.WorldContextRegistry.WorldAddress
import codechicken.multipart.PacketScheduler
import scala.collection.JavaConversions._
import net.minecraft.network.play.{INetHandlerPlayServer, INetHandlerPlayClient}
import net.minecraft.network.play.server.S40PacketDisconnect
import net.minecraft.util.{ChatComponentText, ChatComponentTranslation}
import net.minecraft.network.NetHandlerPlayServer

class MultipartPH {
  val channel = MultipartMod
  val registryChannel = "ForgeMultipart"

  /** Writes the address of a world into the ForgeMultipart payload. */
  def writeWorld(packet: PacketCustom, world: World): PacketCustom = {
    val address = WorldContextRegistry.addressOf(world)
    packet.writeInt(address.hostDimensionId)
    packet.writeInt(address.subId)
    packet.writeString(address.namespace)
    packet
  }

  def readWorldAddress(packet: PacketCustom): WorldAddress = {
    val hostDimensionId = packet.readInt
    val subId = packet.readInt
    val namespace = packet.readString()
    new WorldAddress(hostDimensionId, namespace, subId)
  }
}

object MultipartCPH extends MultipartPH with IClientPacketHandler {
  def handlePacket(
      packet: PacketCustom,
      mc: Minecraft,
      netHandler: INetHandlerPlayClient
  ) {
    try {
      packet.getType match {
        case 1 => handlePartRegistration(packet, netHandler)
        case 2 =>
          handleCompressedTileDesc(
            packet,
            WorldContextRegistry.getClientWorld(readWorldAddress(packet))
          )
        case 3 =>
          handleCompressedTileData(
            packet,
            WorldContextRegistry.getClientWorld(readWorldAddress(packet))
          )
      }
    } catch {
      case e: RuntimeException
          if e.getMessage != null && e.getMessage.startsWith("DC: ") =>
        netHandler.handleDisconnect(
          new S40PacketDisconnect(
            new ChatComponentText(e.getMessage.substring(4))
          )
        )
    }
  }

  def handlePartRegistration(
      packet: PacketCustom,
      netHandler: INetHandlerPlayClient
  ) {
    val missing = MultiPartRegistry.readIDMap(packet)
    if (!missing.isEmpty)
      netHandler.handleDisconnect(
        new S40PacketDisconnect(
          new ChatComponentTranslation(
            "multipart.missing",
            missing.mkString(", ")
          )
        )
      )
  }

  def handleCompressedTileDesc(packet: PacketCustom, world: World) {
    val cc = new ChunkCoordIntPair(packet.readInt, packet.readInt)
    val num = packet.readUShort
    for (i <- 0 until num)
      TileMultipart.handleDescPacket(
        world,
        new BlockCoord(
          packet.readByte + (cc.chunkXPos << 4),
          packet.readInt,
          packet.readByte + (cc.chunkZPos << 4)
        ),
        packet
      )
  }

  def handleCompressedTileData(packet: PacketCustom, world: World) {
    var x = packet.readInt
    while (x != Int.MaxValue) {
      val pos = new BlockCoord(x, packet.readInt, packet.readInt)
      var i = packet.readUByte
      while (i < 255) {
        TileMultipart.handlePacket(pos, world, i, packet)
        i = packet.readUByte
      }
      x = packet.readInt
    }
  }
}

object MultipartSPH
    extends MultipartPH
    with IServerPacketHandler
    with IHandshakeHandler {
  class MCByteStream(bout: ByteArrayOutputStream)
      extends MCDataOutputWrapper(new DataOutputStream(bout)) {
    def getBytes = bout.toByteArray
  }

  private val updateMap = Map[World, Map[BlockCoord, MCByteStream]]()

  private type WatchMap = HashMap[EntityPlayerMP, Set[ChunkCoordIntPair]]
    with MultiMap[EntityPlayerMP, ChunkCoordIntPair]

  private def newWatchMap: WatchMap =
    new HashMap[EntityPlayerMP, Set[ChunkCoordIntPair]]
      with MultiMap[EntityPlayerMP, ChunkCoordIntPair]

  /** Watches are separated by world first, because a chunk coordinate only
    * means something inside one world, and the same player may watch chunks in
    * a host world and in virtual worlds inside it. The world is recorded when
    * the watch happens and never re-derived from the player afterwards.
    */
  private val chunkWatchers = Map[World, WatchMap]()
  private val newWatchers = Map[World, WatchMap]()

  def handlePacket(
      packet: PacketCustom,
      sender: EntityPlayerMP,
      netHandler: INetHandlerPlayServer
  ) {
    packet.getType match {
      case 1 => ControlKeyModifer.map.put(sender, packet.readBoolean)
    }
  }

  def handshakeRecieved(netHandler: NetHandlerPlayServer) {
    val packet = new PacketCustom(registryChannel, 1)
    MultiPartRegistry.writeIDMap(packet)
    netHandler.sendPacket(packet.toPacket)
  }

  def onWorldUnload(world: World) {
    if (!world.isRemote) {
      updateMap.remove(world)
      chunkWatchers.remove(world)
      newWatchers.remove(world)
    }
  }

  def getTileStream(world: World, pos: BlockCoord) =
    updateMap
      .getOrElseUpdate(
        world, {
          if (world.isRemote)
            throw new IllegalArgumentException(
              "Cannot use MultipartSPH on a client world"
            )
          Map()
        }
      )
      .getOrElseUpdate(
        pos, {
          val s = new MCByteStream(new ByteArrayOutputStream)
          s.writeCoord(pos)
          s
        }
      )

  def onTickEnd() {
    PacketScheduler.sendScheduled()

    for (
      (world, m) <- updateMap if !m.isEmpty;
      watchers <- chunkWatchers.get(world);
      (p, chunks) <- watchers
    ) {
      val packet = writeWorld(new PacketCustom(channel, 3).compress(), world)
      var send = false
      for (
        (pos, stream) <- m
        if chunks(new ChunkCoordIntPair(pos.x >> 4, pos.z >> 4))
      ) {
        send = true
        packet.writeByteArray(stream.getBytes)
        packet.writeByte(255) // terminator
      }
      if (send) {
        packet.writeInt(Int.MaxValue) // terminator
        packet.sendToPlayer(p)
      }
    }
    updateMap.foreach(_._2.clear())

    for (
      (world, watchers) <- newWatchers;
      (p, chunks) <- watchers;
      c <- chunks
    ) {
      val chunk = world.getChunkFromChunkCoords(c.chunkXPos, c.chunkZPos)
      val pkt = getDescPacket(
        chunk,
        chunk.chunkTileEntityMap
          .asInstanceOf[JMap[_, TileEntity]]
          .values
          .iterator
      )
      if (pkt != null) pkt.sendToPlayer(p)
      chunkWatchers.getOrElseUpdate(world, newWatchMap).addBinding(p, c)
    }
    newWatchers.clear()
  }

  def onChunkWatch(p: EntityPlayerMP, c: ChunkCoordIntPair) {
    newWatchers.getOrElseUpdate(p.worldObj, newWatchMap).addBinding(p, c)
  }

  def onChunkUnWatch(p: EntityPlayerMP, c: ChunkCoordIntPair) {
    removeBinding(newWatchers, p.worldObj, p, c)
    removeBinding(chunkWatchers, p.worldObj, p, c)
  }

  private def removeBinding(
      map: Map[World, WatchMap],
      world: World,
      p: EntityPlayerMP,
      c: ChunkCoordIntPair
  ) {
    map.get(world) match {
      case Some(watchers) =>
        watchers.removeBinding(p, c)
        if (watchers.isEmpty) map.remove(world)
      case _ =>
    }
  }
  def onPlayerLogout(p: EntityPlayer) = p match {
    case mp: EntityPlayerMP =>
      for (map <- Seq(chunkWatchers, newWatchers)) {
        for ((world, watchers) <- map.toSeq) {
          watchers.remove(mp)
          if (watchers.isEmpty) map.remove(world)
        }
      }
    case _ =>
  }

  def getDescPacket(chunk: Chunk, it: Iterator[TileEntity]): PacketCustom = {
    val s = new MCByteStream(new ByteArrayOutputStream)

    var num = 0
    while (it.hasNext) {
      val tile = it.next
      if (tile.isInstanceOf[TileMultipart]) {
        s.writeByte(tile.xCoord & 0xf)
        s.writeInt(tile.yCoord)
        s.writeByte(tile.zCoord & 0xf)
        tile.asInstanceOf[TileMultipart].writeDesc(s)
        num += 1
      }
    }
    if (num != 0) {
      return writeWorld(new PacketCustom(channel, 2).compress(), chunk.worldObj)
        .writeInt(chunk.xPosition)
        .writeInt(chunk.zPosition)
        .writeShort(num)
        .writeByteArray(s.getBytes)
    }
    return null
  }
}
