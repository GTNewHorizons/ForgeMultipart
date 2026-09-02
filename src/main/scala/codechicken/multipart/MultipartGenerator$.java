package codechicken.multipart;

import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.asm.ASMImplicits;
import codechicken.multipart.asm.MultipartMixinFactory;
import codechicken.multipart.asm.ScratchBitSet;
import codechicken.multipart.asm.ScratchBitSet$class;
import codechicken.multipart.handler.MultipartProxy;
import codechicken.multipart.handler.MultipartProxy$;
import scala.Option;
import scala.Tuple2;
import scala.collection.Iterable;
import scala.collection.Iterator;
import scala.collection.immutable.Nil$;
import scala.collection.mutable.HashMap;
import scala.collection.mutable.Map;

public final class MultipartGenerator$ implements ScratchBitSet {

    public static final MultipartGenerator$ MODULE$ = new MultipartGenerator$();

    private final Map<Class<?>, BitSet> tileTraitMap = new HashMap<>();
    private final Map<String, String> interfaceTraitMap_c = new HashMap<>();
    private final Map<String, String> interfaceTraitMap_s = new HashMap<>();
    private final Map<Class<?>, BitSet> partTraitMap_c = new HashMap<>();
    private final Map<Class<?>, BitSet> partTraitMap_s = new HashMap<>();
    private final int clientTraitId;
    private ThreadLocal<BitSet> codechicken$multipart$asm$ScratchBitSet$$bitSets;

    private MultipartGenerator$() {
        ScratchBitSet$class.$init$(this);
        clientTraitId = MultipartMixinFactory.registerTrait("codechicken.multipart.TileMultipartClient");
    }

    @Override
    public ThreadLocal<BitSet> codechicken$multipart$asm$ScratchBitSet$$bitSets() {
        return codechicken$multipart$asm$ScratchBitSet$$bitSets;
    }

    @Override
    public void codechicken$multipart$asm$ScratchBitSet$_setter_$codechicken$multipart$asm$ScratchBitSet$$bitSets_$eq(
            ThreadLocal value) {
        codechicken$multipart$asm$ScratchBitSet$$bitSets = value;
    }

    @Override
    public BitSet getBitSet() {
        return ScratchBitSet$class.getBitSet(this);
    }

    @Override
    public BitSet freshBitSet() {
        return ScratchBitSet$class.freshBitSet(this);
    }

    public Map<String, String> codechicken$multipart$MultipartGenerator$$interfaceTraitMap(boolean client) {
        return client ? interfaceTraitMap_c : interfaceTraitMap_s;
    }

    public BitSet codechicken$multipart$MultipartGenerator$$traitsForPart(TMultiPart part, boolean client) {
        Map<Class<?>, BitSet> cache = client ? partTraitMap_c : partTraitMap_s;
        Option<BitSet> cached = cache.get(part.getClass());
        if (cached.isDefined()) return cached.get();

        Set<String> traits = new LinkedHashSet<>();
        collectTraits(part.getClass(), codechicken$multipart$MultipartGenerator$$interfaceTraitMap(client), traits);
        BitSet bitset = new BitSet();
        for (String trait : traits) bitset.set(MultipartMixinFactory.getId(trait));
        cache.put(part.getClass(), bitset);
        return bitset;
    }

    private void collectTraits(Class<?> type, Map<String, String> map, Set<String> traits) {
        for (Class<?> iface : type.getInterfaces()) collectTraits(iface, map, traits);
        Option<String> trait = map.get(ASMImplicits.nodeName(type.getName()));
        if (trait.isDefined()) traits.add(trait.get());
        if (type.getSuperclass() != null) collectTraits(type.getSuperclass(), map, traits);
    }

    private BitSet setTraits(TMultiPart part, boolean client) {
        BitSet bitset = freshBitSet();
        bitset.or(codechicken$multipart$MultipartGenerator$$traitsForPart(part, client));
        if (client) bitset.set(clientTraitId);
        return bitset;
    }

    private BitSet setTraits(Iterable<TMultiPart> parts, boolean client) {
        BitSet bitset = freshBitSet();
        for (Iterator<TMultiPart> it = parts.iterator(); it.hasNext();) {
            bitset.or(codechicken$multipart$MultipartGenerator$$traitsForPart(it.next(), client));
        }
        if (client) bitset.set(clientTraitId);
        return bitset;
    }

    /** Replaces the tile if the new part requires additional interfaces, then adds the part. */
    public TileMultipart addPart(World world, BlockCoord pos, TMultiPart part) {
        Tuple2<TileMultipart, Object> convertedTile = TileMultipart.getOrConvertTile2(world, pos);
        TileMultipart tile = convertedTile._1();
        boolean converted = (Boolean) convertedTile._2();
        BitSet bitset = setTraits(part, world.isRemote);

        TileMultipart next = tile;
        if (next != null) {
            if (converted) {
                next.partList().apply(0).invalidateConvertedTile();
                world.setBlock(pos.x, pos.y, pos.z, MultipartProxy.block(), 0, 0);
                silentAddTile(world, pos, next);
                PacketCustom.sendToChunk(
                        new S23PacketBlockChange(pos.x, pos.y, pos.z, world),
                        world,
                        pos.x >> 4,
                        pos.z >> 4);
                next.partList().apply(0).onConverted();
                next.writeAddPart(next.partList().apply(0));
            }

            BitSet tileTraits = tileTraitMap.apply(tile.getClass());
            bitset.andNot(tileTraits);
            if (!bitset.isEmpty()) {
                bitset.or(tileTraits);
                next = (TileMultipart) MultipartMixinFactory.construct(bitset, Nil$.MODULE$);
                tile.setValid(false);
                silentAddTile(world, pos, next);
                next.from(tile);
            }
        } else {
            world.setBlock(pos.x, pos.y, pos.z, MultipartProxy.block(), 0, 0);
            next = (TileMultipart) MultipartMixinFactory.construct(bitset, Nil$.MODULE$);
            silentAddTile(world, pos, next);
        }
        next.addPart_impl(part);
        return next;
    }

    public void silentAddTile(World world, BlockCoord pos, TileEntity tile) {
        Chunk chunk = world.getChunkFromBlockCoords(pos.x, pos.z);
        if (chunk != null) chunk.func_150812_a(pos.x & 15, pos.y, pos.z & 15, tile);
    }

    /** Reuses the tile only when it has exactly the interfaces required by the parts. */
    public TileMultipart generateCompositeTile(TileEntity tile, Iterable<TMultiPart> parts, boolean client) {
        BitSet bitset = setTraits(parts, client);
        if (tile instanceof TileMultipart && bitset.equals(tileTraitMap.apply(tile.getClass()))) {
            return (TileMultipart) tile;
        }
        return (TileMultipart) MultipartMixinFactory.construct(bitset, Nil$.MODULE$);
    }

    /** Replaces a tile whose remaining parts no longer require all its interfaces. */
    public TileMultipart partRemoved(TileMultipart tile) {
        TileMultipart next = generateCompositeTile(tile, tile.partList(), tile.getWorldObj().isRemote);
        if (!next.equals(tile)) {
            tile.setValid(false);
            silentAddTile(tile.getWorldObj(), new BlockCoord(tile), next);
            next.from(tile);
            next.notifyTileChange();
        }
        return next;
    }

    public void registerTrait(String marker, String trait) {
        registerTrait(marker, trait, trait);
    }

    public void registerTrait(String marker, String clientTrait, String serverTrait) {
        marker = ASMImplicits.nodeName(marker);
        clientTrait = ASMImplicits.nodeName(clientTrait);
        serverTrait = ASMImplicits.nodeName(serverTrait);
        registerSide(interfaceTraitMap_c, marker, clientTrait);
        registerSide(interfaceTraitMap_s, marker, serverTrait);
    }

    private void registerSide(Map<String, String> map, String marker, String trait) {
        if (trait == null) return;
        if (map.contains(marker)) {
            MultipartProxy.logger().error("Trait already registered for " + marker);
        } else {
            map.put(marker, trait);
            MultipartMixinFactory.registerTrait(trait);
        }
    }

    public void registerPassThroughInterface(String name) {
        registerPassThroughInterface(name, true, true);
    }

    public void registerPassThroughInterface(String name, boolean client, boolean server) {
        String trait = MultipartMixinFactory.generatePassThroughTrait(name);
        if (trait == null) return;
        registerTrait(name, client ? trait : null, server ? trait : null);
    }

    public void registerTileClass(Class<? extends TileMultipart> type, BitSet traits) {
        BitSet copy = new BitSet();
        copy.or(traits);
        tileTraitMap.put(type, copy);
        MultipartProxy$.MODULE$.onTileClassBuilt(type);
    }
}
