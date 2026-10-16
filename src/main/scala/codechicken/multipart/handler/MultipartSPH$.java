package codechicken.multipart.handler;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.LinkedList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustom.IHandshakeHandler;
import codechicken.lib.packet.PacketCustom.IServerPacketHandler;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.ControlKeyModifer;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.PacketScheduler;
import codechicken.multipart.TileMultipart;
import scala.MatchError;
import scala.Option;
import scala.Tuple2;
import scala.collection.mutable.HashMap;
import scala.collection.mutable.HashSet;
import scala.collection.mutable.Map;
import scala.collection.mutable.Set;

public final class MultipartSPH$ extends MultipartPH implements IServerPacketHandler, IHandshakeHandler {

    public static final MultipartSPH$ MODULE$ = new MultipartSPH$();

    private final Map<World, Map<BlockCoord, MultipartSPH.MCByteStream>> codechicken$multipart$handler$MultipartSPH$$updateMap = new HashMap<>();
    private final HashMap<Object, Set<ChunkCoordIntPair>> codechicken$multipart$handler$MultipartSPH$$chunkWatchers = new HashMap<>();
    private final Map<Object, LinkedList<ChunkCoordIntPair>> codechicken$multipart$handler$MultipartSPH$$newWatchers = new HashMap<>();

    private MultipartSPH$() {}

    public Map<World, Map<BlockCoord, MultipartSPH.MCByteStream>> codechicken$multipart$handler$MultipartSPH$$updateMap() {
        return codechicken$multipart$handler$MultipartSPH$$updateMap;
    }

    public HashMap<Object, Set<ChunkCoordIntPair>> codechicken$multipart$handler$MultipartSPH$$chunkWatchers() {
        return codechicken$multipart$handler$MultipartSPH$$chunkWatchers;
    }

    public Map<Object, LinkedList<ChunkCoordIntPair>> codechicken$multipart$handler$MultipartSPH$$newWatchers() {
        return codechicken$multipart$handler$MultipartSPH$$newWatchers;
    }

    @Override
    public void handlePacket(PacketCustom packet, EntityPlayerMP sender, INetHandlerPlayServer netHandler) {
        int type = packet.getType();
        if (type == 1) {
            ControlKeyModifer.map().put(sender, packet.readBoolean());
            return;
        }
        throw new MatchError(type);
    }

    @Override
    public void handshakeRecieved(NetHandlerPlayServer netHandler) {
        PacketCustom packet = new PacketCustom(registryChannel(), 1);
        MultiPartRegistry.writeIDMap(packet);
        netHandler.sendPacket(packet.toPacket());
    }

    public void onWorldUnload(World world) {
        if (!world.isRemote) {
            codechicken$multipart$handler$MultipartSPH$$updateMap.remove(world);
        }
    }

    public MultipartSPH.MCByteStream getTileStream(World world, BlockCoord position) {
        Option<Map<BlockCoord, MultipartSPH.MCByteStream>> worldOption = codechicken$multipart$handler$MultipartSPH$$updateMap
                .get(world);
        Map<BlockCoord, MultipartSPH.MCByteStream> worldUpdates;
        if (worldOption.isEmpty()) {
            if (world.isRemote) {
                throw new IllegalArgumentException("Cannot use MultipartSPH on a client world");
            }
            worldUpdates = new HashMap<>();
            codechicken$multipart$handler$MultipartSPH$$updateMap.put(world, worldUpdates);
        } else {
            worldUpdates = worldOption.get();
        }

        Option<MultipartSPH.MCByteStream> streamOption = worldUpdates.get(position);
        if (!streamOption.isEmpty()) {
            return streamOption.get();
        }

        MultipartSPH.MCByteStream stream = new MultipartSPH.MCByteStream(new ByteArrayOutputStream());
        stream.writeCoord(position);
        worldUpdates.put(position, stream);
        return stream;
    }

    public void onTickEnd(scala.collection.Seq<EntityPlayerMP> players) {
        PacketScheduler.sendScheduled();

        scala.collection.Iterator<EntityPlayerMP> playerIterator = players.iterator();
        while (playerIterator.hasNext()) {
            EntityPlayerMP player = playerIterator.next();
            Object playerId = player.getEntityId();
            Option<Set<ChunkCoordIntPair>> chunkOption = codechicken$multipart$handler$MultipartSPH$$chunkWatchers
                    .get(playerId);
            if (chunkOption.isEmpty()) {
                continue;
            }

            Option<Map<BlockCoord, MultipartSPH.MCByteStream>> updatesOption = codechicken$multipart$handler$MultipartSPH$$updateMap
                    .get(player.worldObj);
            if (updatesOption.isEmpty() || updatesOption.get().isEmpty()) {
                continue;
            }

            Set<ChunkCoordIntPair> chunks = chunkOption.get();
            PacketCustom packet = new PacketCustom(channel(), 3).compress();
            boolean send = false;
            scala.collection.Iterator<Tuple2<BlockCoord, MultipartSPH.MCByteStream>> updateIterator = updatesOption
                    .get().iterator();
            while (updateIterator.hasNext()) {
                Tuple2<BlockCoord, MultipartSPH.MCByteStream> update = updateIterator.next();
                BlockCoord position = update._1();
                if (chunks.contains(new ChunkCoordIntPair(position.x >> 4, position.z >> 4))) {
                    send = true;
                    packet.writeByteArray(update._2().getBytes());
                    packet.writeByte(255);
                }
            }
            if (send) {
                packet.writeInt(Integer.MAX_VALUE);
                packet.sendToPlayer(player);
            }
        }

        scala.collection.Iterator<Tuple2<World, Map<BlockCoord, MultipartSPH.MCByteStream>>> worldIterator = codechicken$multipart$handler$MultipartSPH$$updateMap
                .iterator();
        while (worldIterator.hasNext()) {
            worldIterator.next()._2().clear();
        }

        playerIterator = players.iterator();
        while (playerIterator.hasNext()) {
            EntityPlayerMP player = playerIterator.next();
            Object playerId = player.getEntityId();
            Option<LinkedList<ChunkCoordIntPair>> chunksOption = codechicken$multipart$handler$MultipartSPH$$newWatchers
                    .get(playerId);
            if (chunksOption.isEmpty()) {
                continue;
            }

            for (ChunkCoordIntPair chunkPosition : chunksOption.get()) {
                Chunk chunk = player.worldObj.getChunkFromChunkCoords(chunkPosition.chunkXPos, chunkPosition.chunkZPos);
                @SuppressWarnings("unchecked")
                Iterator<TileEntity> tiles = ((java.util.Map<Object, TileEntity>) chunk.chunkTileEntityMap).values()
                        .iterator();
                PacketCustom packet = getDescPacket(chunk, tiles);
                if (packet != null) {
                    packet.sendToPlayer(player);
                }
                addChunkWatcher(playerId, chunkPosition);
            }
        }
        codechicken$multipart$handler$MultipartSPH$$newWatchers.clear();
    }

    public void onChunkWatch(EntityPlayer player, ChunkCoordIntPair chunk) {
        Object playerId = player.getEntityId();
        Option<LinkedList<ChunkCoordIntPair>> option = codechicken$multipart$handler$MultipartSPH$$newWatchers
                .get(playerId);
        LinkedList<ChunkCoordIntPair> chunks;
        if (option.isEmpty()) {
            chunks = new LinkedList<>();
            codechicken$multipart$handler$MultipartSPH$$newWatchers.put(playerId, chunks);
        } else {
            chunks = option.get();
        }
        chunks.add(chunk);
    }

    public void onChunkUnWatch(EntityPlayer player, ChunkCoordIntPair chunk) {
        Object playerId = player.getEntityId();
        Option<LinkedList<ChunkCoordIntPair>> pending = codechicken$multipart$handler$MultipartSPH$$newWatchers
                .get(playerId);
        if (!pending.isEmpty()) {
            pending.get().remove(chunk);
        }

        Option<Set<ChunkCoordIntPair>> active = codechicken$multipart$handler$MultipartSPH$$chunkWatchers.get(playerId);
        if (!active.isEmpty()) {
            Set<ChunkCoordIntPair> chunks = active.get();
            chunks.remove(chunk);
            if (chunks.isEmpty()) {
                codechicken$multipart$handler$MultipartSPH$$chunkWatchers.remove(playerId);
            }
        }
    }

    public PacketCustom getDescPacket(Chunk chunk, Iterator<TileEntity> tiles) {
        MultipartSPH.MCByteStream stream = new MultipartSPH.MCByteStream(new ByteArrayOutputStream());
        int count = 0;
        while (tiles.hasNext()) {
            TileEntity tile = tiles.next();
            if (tile instanceof TileMultipart) {
                stream.writeByte(tile.xCoord & 0xF);
                stream.writeInt(tile.yCoord);
                stream.writeByte(tile.zCoord & 0xF);
                ((TileMultipart) tile).writeDesc(stream);
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return new PacketCustom(channel(), 2).compress().writeInt(chunk.xPosition).writeInt(chunk.zPosition)
                .writeShort(count).writeByteArray(stream.getBytes());
    }

    private void addChunkWatcher(Object playerId, ChunkCoordIntPair chunk) {
        Option<Set<ChunkCoordIntPair>> option = codechicken$multipart$handler$MultipartSPH$$chunkWatchers.get(playerId);
        Set<ChunkCoordIntPair> chunks;
        if (option.isEmpty()) {
            chunks = new HashSet<>();
            codechicken$multipart$handler$MultipartSPH$$chunkWatchers.put(playerId, chunks);
        } else {
            chunks = option.get();
        }
        chunks.add(chunk);
    }
}
