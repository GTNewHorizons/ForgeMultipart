package codechicken.multipart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import codechicken.lib.data.MCDataOutput;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.lib.world.IChunkLoadTile;
import codechicken.multipart.handler.MultipartCompatiblity;
import codechicken.multipart.handler.MultipartProxy;
import codechicken.multipart.handler.MultipartSPH;
import scala.Function1;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

public class TileMultipart extends TileEntity implements IChunkLoadTile {

    /** List of parts in this tile space. */
    private Seq<TMultiPart> partList = emptyParts();

    private boolean doesTick = false;

    public Seq<TMultiPart> partList() {
        return partList;
    }

    public void partList_$eq(Seq<TMultiPart> parts) {
        partList = parts;
    }

    public void from(TileMultipart that) {
        copyFrom(that);
        loadFrom(that);
    }

    /**
     * This method should be used for copying all the data from the fields in that container tile. This method will be
     * automatically generated on java tile traits with fields if it is not overridden.
     */
    public void copyFrom(TileMultipart that) {
        partList = that.partList;
        doesTick = that.doesTick;
    }

    public void loadFrom(TileMultipart that) {
        Iterator<TMultiPart> iterator = partList.iterator();
        while (iterator.hasNext()) {
            iterator.next().bind(this);
        }
        if (doesTick) {
            doesTick = false;
            setTicking(true);
        }
    }

    /** Overidden in TSlottedTile when a part that goes in a slot is added. */
    public TMultiPart partMap(int slot) {
        return null;
    }

    /** Implicit java conversion of part list. */
    public List<TMultiPart> jPartList() {
        return JavaConversions.seqAsJavaList(partList);
    }

    @Override
    public boolean canUpdate() {
        return doesTick;
    }

    // Direct list traversal avoids measured iterator/wrapper allocations; the setter also accepts other Seq types.
    @SuppressWarnings("unchecked")
    public void operate(Function1<TMultiPart, BoxedUnit> f) {
        Seq<TMultiPart> current = partList;
        if (!(current instanceof scala.collection.immutable.List)) {
            scala.collection.Iterator<TMultiPart> iterator = current.iterator();
            while (iterator.hasNext()) {
                applyIfBound(f, iterator.next());
            }
            return;
        }

        scala.collection.immutable.List<TMultiPart> list = (scala.collection.immutable.List<TMultiPart>) current;
        while (!list.isEmpty()) {
            TMultiPart p = list.head();
            list = (scala.collection.immutable.List<TMultiPart>) list.tail();
            applyIfBound(f, p);
        }
    }

    private static void applyIfBound(Function1<TMultiPart, BoxedUnit> f, TMultiPart p) {
        if (p.tile() != null) {
            f.apply(p);
        }
    }

    @Override
    public void updateEntity() {
        operate(action(TMultiPart::update));
    }

    @Override
    public void onChunkUnload() {
        operate(action(TMultiPart::onChunkUnload));
    }

    @Override
    public void onChunkLoad() {
        operate(action(TMultiPart::onChunkLoad));
    }

    public final void setValid(boolean b) {
        if (b) {
            super.validate();
        } else {
            super.invalidate();
        }
    }

    public void onMoved() {
        operate(action(TMultiPart::onMoved));
    }

    @Override
    public void invalidate() {
        if (!isInvalid()) {
            super.invalidate();
            if (worldObj != null) {
                Iterator<TMultiPart> iterator = partList.iterator();
                while (iterator.hasNext()) {
                    iterator.next().onWorldSeparate();
                }
                if (worldObj.isRemote) {
                    TileCache.remove(this);
                }
            }
        }
    }

    @Override
    public void validate() {
        super.validate();

        if (worldObj != null && worldObj.isRemote) {
            TileCache.add(this);
        }
    }

    /**
     * Called by parts when they have changed in some form that affects the world. Notifies neighbor blocks, the world
     * and parts that share this host and recalculates lighting.
     */
    public void notifyPartChange(TMultiPart part) {
        internalPartChange(part);

        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, MultipartProxy.block());
        worldObj.func_147451_t(xCoord, yCoord, zCoord);
    }

    /** Notifies parts sharing this host of a change. */
    public void internalPartChange(TMultiPart part) {
        operate(action(p -> {
            if (part == null ? p != null : !part.equals(p)) {
                p.onPartChanged(part);
            }
        }));
    }

    /** Notifies all parts not in the passed collection of a change from all the parts in the collection. */
    public void multiPartChange(Collection<TMultiPart> parts) {
        operate(action(p -> {
            if (!parts.contains(p)) {
                for (TMultiPart changed : parts) {
                    p.onPartChanged(changed);
                }
            }
        }));
    }

    /** Notifies neighboring blocks that this tile has changed. */
    public void notifyTileChange() {
        worldObj.func_147453_f(xCoord, yCoord, zCoord, MultipartProxy.block());
    }

    public void onNeighborBlockChange() {
        operate(action(TMultiPart::onNeighborChanged));
    }

    /** Blank implementation, overriden by TTileChangeTile. */
    public void onNeighborTileChange(int tileX, int tileY, int tileZ) {}

    /** Blank implementation, overriden by TTileChangeTile. */
    public boolean getWeakChanges() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public int getLightValue() {
        int max = 0;
        Seq<TMultiPart> current = partList;
        if (!(current instanceof scala.collection.immutable.List)) {
            Iterator<TMultiPart> iterator = current.iterator();
            while (iterator.hasNext()) {
                max = Math.max(max, iterator.next().getLightValue());
            }
            return max;
        }

        scala.collection.immutable.List<TMultiPart> list = (scala.collection.immutable.List<TMultiPart>) current;
        while (!list.isEmpty()) {
            max = Math.max(max, list.head().getLightValue());
            list = (scala.collection.immutable.List<TMultiPart>) list.tail();
        }
        return max;
    }

    public float getExplosionResistance(Entity entity) {
        if (partList.isEmpty()) {
            // Matches the reference, where max on an empty view throws.
            throw new UnsupportedOperationException("empty.max");
        }
        float max = Float.NEGATIVE_INFINITY;
        Iterator<TMultiPart> iterator = partList.iterator();
        while (iterator.hasNext()) {
            max = Math.max(max, iterator.next().explosionResistance(entity));
        }
        return max;
    }

    /** Callback for parts to mark the chunk as needs saving. */
    @Override
    public void markDirty() {
        worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this);
    }

    /** Mark this block space for a render update. */
    public void markRender() {
        worldObj.func_147479_m(xCoord, yCoord, zCoord);
    }

    /** Stable class-call target for the flag getter generated by the TileMultipartClient runtime interface. */
    public boolean hasDynamicParts() {
        return false;
    }

    /** Client render hook overridden by generated tiles carrying TileMultipartClient. */
    public boolean renderStatic(IBlockAccess world, Vector3 position, RenderBlocks renderer) {
        return false;
    }

    /** Client render hook overridden by generated tiles carrying TileMultipartClient. */
    public void renderDynamic(Vector3 position, float frame, int pass) {}

    /** Client particle hook overridden by generated tiles carrying TRandomDisplayTickTile. */
    public void randomDisplayTick(Random random) {}

    /** Helper function for calling a second level notify on a side (eg indirect power from a lever). */
    public void notifyNeighborChange(int side) {
        BlockCoord pos = new BlockCoord(this).offset(side);
        worldObj.notifyBlocksOfNeighborChange(pos.x, pos.y, pos.z, MultipartProxy.block());
    }

    public boolean isSolid(int side) {
        TMultiPart part = partMap(side);
        return part instanceof TFacePart && ((TFacePart) part).solid(side);
    }

    public boolean canPlaceTorchOnTop() {
        Iterator<TMultiPart> iterator = partList.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().canPlaceTorchOnTop()) {
                return true;
            }
        }
        return isSolid(1);
    }

    private void setTicking(boolean tick) {
        if (doesTick == tick) {
            return;
        }

        doesTick = tick;
        if (worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this) {
            if (tick) {
                worldObj.addTileEntity(this);
            } else {
                worldObj.func_147457_a(this);
            }
        }
    }

    /** Returns true if part can be added to this space. */
    public boolean canAddPart(TMultiPart part) {
        return compatibilityAllows(worldObj, xCoord, yCoord, zCoord) && !partList.contains(part)
                && occlusionTest(partList, part);
    }

    /**
     * Returns true if opart can be replaced with npart (note opart and npart may be the exact same object).
     * <p>
     * This function should be used for testing if a part can change it's shape (eg. rotation, expansion, cable
     * connection). For example, to test whether a cable part can connect to it's neighbor: set the cable part's
     * bounding boxes as if the connection is established, call canReplacePart(part, part), and if it succeeds perform
     * the connection, else revert the bounding box.
     */
    public boolean canReplacePart(TMultiPart opart, TMultiPart npart) {
        List<TMultiPart> olist = new ArrayList<>();
        Iterator<TMultiPart> iterator = partList.iterator();
        while (iterator.hasNext()) {
            TMultiPart part = iterator.next();
            if (part == null ? opart != null : !part.equals(opart)) {
                olist.add(part);
            }
        }
        Seq<TMultiPart> others = toSeq(olist);
        if (others.contains(npart)) {
            return false;
        }

        return occlusionTest(others, npart);
    }

    /** Returns true if parts do not occlude npart. */
    public boolean occlusionTest(Seq<TMultiPart> parts, TMultiPart npart) {
        for (TMultiPart part : JavaConversions.seqAsJavaList(parts)) {
            if (!part.occlusionTest(npart) || !npart.occlusionTest(part)) {
                return false;
            }
        }
        return true;
    }

    /** Get the write stream for updates to part. */
    public MCDataOutput getWriteStream(TMultiPart part) {
        return writeStream().writeByte(partList.indexOf(part));
    }

    private MCDataOutput writeStream() {
        return MultipartSPH.getTileStream(worldObj, new BlockCoord(this));
    }

    public void addPart_impl(TMultiPart part) {
        if (!worldObj.isRemote) {
            writeAddPart(part);
        }

        addPart_do(part);
        part.onAdded();
        partAdded(part);
        notifyPartChange(part);
        notifyTileChange();
        markDirty();
        markRender();
    }

    public void writeAddPart(TMultiPart part) {
        MCDataOutput stream = writeStream().writeByte(253);
        MultiPartRegistry.writePartID(stream, part);
        part.writeDesc(stream);
    }

    public void addPart_do(TMultiPart part) {
        if (partList.size() >= 250) {
            throw new AssertionError(
                    "assertion failed: Tried to add more than 250 parts to the one tile. You're doing it wrong");
        }

        List<TMultiPart> next = mutablePartsSnapshot();
        next.add(part);
        partList = toSeq(next);
        bindPart(part);
        part.bind(this);

        if (!doesTick && part.doesTick()) {
            setTicking(true);
        }
    }

    /** Bind this part to an internal cache. Provided for trait overrides, do not call externally. */
    public void bindPart(TMultiPart part) {}

    /** Called when a part is added (placement). Provided for trait overrides, do not call externally. */
    public void partAdded(TMultiPart part) {}

    /**
     * Removes part from this tile. Note that due to the operation sync, the part may not be removed until the call
     * stack has been passed to all other parts in the space.
     */
    public TileMultipart remPart(TMultiPart part) {
        if (worldObj.isRemote) {
            throw new AssertionError("assertion failed: Cannot remove multi parts from a client tile");
        }
        return remPart_impl(part);
    }

    public TileMultipart remPart_impl(TMultiPart part) {
        remPart_do(part, !worldObj.isRemote);

        if (!isInvalid()) {
            TileMultipart tile = MultipartGenerator$.MODULE$.partRemoved(this);
            tile.notifyPartChange(part);
            tile.markDirty();
            tile.markRender();
            return tile;
        }

        return null;
    }

    private int remPart_do(TMultiPart part, boolean sendPacket) {
        int r = partList.indexOf(part);
        if (r < 0) {
            throw new IllegalArgumentException("Tried to remove a non-existant part");
        }

        // The reference read an unused removedLightValue here; the virtual call is observable, so keep it.
        part.getLightValue();
        part.preRemove();
        List<TMultiPart> current = mutablePartsSnapshot();
        current.removeIf(p -> p == null ? part == null : p.equals(part));
        partList = toSeq(current);

        if (sendPacket) {
            writeStream().writeByte(254).writeByte(r);
        }

        partRemoved(part, r);
        part.onRemoved();
        part.tile_$eq(null);

        if (partList.isEmpty()) {
            worldObj.setBlockToAir(xCoord, yCoord, zCoord);
        } else if (part.doesTick() && doesTick) {
            boolean ntick = false;
            Iterator<TMultiPart> iterator = partList.iterator();
            while (iterator.hasNext()) {
                ntick |= iterator.next().doesTick();
            }
            if (!ntick) {
                setTicking(false);
            }
        }
        return r;
    }

    /** Remove this part from internal cache. Provided for trait overrides, do not call externally. */
    public void partRemoved(TMultiPart part, int p) {}

    public void loadParts(scala.collection.Iterable<TMultiPart> parts) {
        clearParts();
        for (TMultiPart p : JavaConversions.asJavaIterable(parts)) {
            addPart_do(p);
        }

        if (worldObj != null) {
            if (worldObj.isRemote) {
                operate(action(TMultiPart::onWorldJoin));
            }
            notifyPartChange(null);
        }
    }

    /** Remove all parts from internal cache. Provided for trait overrides, do not call externally. */
    public void clearParts() {
        partList = emptyParts();
    }

    /** Writes the description of this tile, and all parts composing it, to packet. */
    public void writeDesc(MCDataOutput packet) {
        packet.writeByte(partList.size());
        Iterator<TMultiPart> iterator = partList.iterator();
        while (iterator.hasNext()) {
            TMultiPart part = iterator.next();
            MultiPartRegistry.writePartID(packet, part);
            part.writeDesc(packet);
        }
    }

    /** Perform a raytrace returning all intersecting parts sorted nearest to farthest. */
    public Iterable<ExtendedMOP> rayTraceAll(Vec3 start, Vec3 end) {
        List<ExtendedMOP> list = new ArrayList<>();
        Iterator<TMultiPart> iterator = partList.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            Object mop = iterator.next().collisionRayTrace(start, end);
            if (mop instanceof ExtendedMOP) {
                ExtendedMOP extended = (ExtendedMOP) mop;
                extended.data = new Tuple2<>(i, extended.data);
                list.add(extended);
            }
            i++;
        }

        Collections.sort(list);
        return list;
    }

    /** Perform a raytrace returning the nearest intersecting part. */
    public ExtendedMOP collisionRayTrace(Vec3 start, Vec3 end) {
        for (ExtendedMOP mop : rayTraceAll(start, end)) {
            return mop;
        }
        return null;
    }

    /** Drop and remove part at index (internal mining callback). */
    public void harvestPart(int index, ExtendedMOP hit, EntityPlayer player) {
        TMultiPart part = partList.apply(index);
        if (part != null) {
            part.harvest(hit, player);
        }
    }

    /** Utility function for dropping items around the center of this space. */
    public void dropItems(Iterable<ItemStack> items) {
        Vector3 pos = Vector3.fromTileEntityCenter(this);
        for (ItemStack item : items) {
            dropItem(item, worldObj, pos);
        }
    }

    @Override
    public final void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList taglist = new NBTTagList();
        Iterator<TMultiPart> iterator = partList.iterator();
        while (iterator.hasNext()) {
            TMultiPart part = iterator.next();
            NBTTagCompound parttag = new NBTTagCompound();
            parttag.setString("id", part.getType());
            part.save(parttag);
            taglist.appendTag(parttag);
        }
        tag.setTag("parts", taglist);
    }

    /** Internal callback. */
    public void onEntityCollision(Entity entity) {
        operate(action(p -> p.onEntityCollision(entity)));
    }

    /** Internal callback, overriden in TRedstoneTile. */
    public int strongPowerLevel(int side) {
        return 0;
    }

    /** Internal callback, overriden in TRedstoneTile. */
    public int weakPowerLevel(int side) {
        return 0;
    }

    /** Internal callback, overriden in TRedstoneTile. */
    public boolean canConnectRedstone(int side) {
        return false;
    }

    /** Mutable snapshot used only while publishing a replacement Seq. */
    private List<TMultiPart> mutablePartsSnapshot() {
        return new ArrayList<>(JavaConversions.seqAsJavaList(partList));
    }

    private static Seq<TMultiPart> toSeq(List<TMultiPart> parts) {
        return JavaConversions.asScalaBuffer(parts).toList();
    }

    private static Seq<TMultiPart> emptyParts() {
        return toSeq(Collections.emptyList());
    }

    /** Wraps a Java action as the Scala function operate takes, so overrides of operate still see every call. */
    private static Function1<TMultiPart, BoxedUnit> action(Consumer<TMultiPart> action) {
        return new AbstractFunction1<TMultiPart, BoxedUnit>() {

            @Override
            public BoxedUnit apply(TMultiPart part) {
                action.accept(part);
                return BoxedUnit.UNIT;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static boolean compatibilityAllows(World world, int x, int y, int z) {
        return (Boolean) MultipartCompatiblity.canAddPart()
                .apply(world, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z));
    }

    // Static helpers, formerly the TileMultipart companion object.

    private static int renderID = -1;

    public static int renderID() {
        return renderID;
    }

    public static void renderID_$eq(int value) {
        renderID = value;
    }

    /** Gets a multipart tile instance at pos, converting if necessary. */
    public static TileMultipart getOrConvertTile(World world, BlockCoord pos) {
        return getOrConvertTile2(world, pos)._1();
    }

    /**
     * Gets a multipart tile instance at pos, converting if necessary. Note converted tiles are merely a structure
     * formality, they do not actually exist in world until they are required to by the addition of another multipart to
     * their space.
     *
     * @return the tile or null if there was none, and true if the tile is a result of a conversion
     */
    public static Tuple2<TileMultipart, Object> getOrConvertTile2(World world, BlockCoord pos) {
        TileEntity t = world.getTileEntity(pos.x, pos.y, pos.z);
        if (t instanceof TileMultipart) {
            return new Tuple2<>((TileMultipart) t, Boolean.FALSE);
        }

        TMultiPart p = MultiPartRegistry.convertBlock(world, pos, world.getBlock(pos.x, pos.y, pos.z));
        if (p != null) {
            TileMultipart tile = MultipartGenerator$.MODULE$
                    .generateCompositeTile(null, toSeq(Collections.singletonList(p)), world.isRemote);
            tile.xCoord = pos.x;
            tile.yCoord = pos.y;
            tile.zCoord = pos.z;
            tile.setWorldObj(world);
            tile.addPart_do(p);
            return new Tuple2<>(tile, Boolean.TRUE);
        }
        return new Tuple2<>(null, Boolean.FALSE);
    }

    /** Gets the multipart tile instance at pos, or null if it doesn't exist or is not a multipart tile. */
    public static TileMultipart getTile(World world, BlockCoord pos) {
        TileEntity t = world.getTileEntity(pos.x, pos.y, pos.z);
        return t instanceof TileMultipart ? (TileMultipart) t : null;
    }

    public static boolean checkNoEntityCollision(World world, BlockCoord pos, TMultiPart part) {
        for (Cuboid6 b : part.getCollisionBoxes()) {
            if (!world.checkNoEntityCollision(b.toAABB().offset(pos.x, pos.y, pos.z))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether part can be added to the space at pos. Will do conversions as necessary. This function is the
     * recommended way to add parts to the world.
     */
    public static boolean canPlacePart(World world, BlockCoord pos, TMultiPart part) {
        if (!checkNoEntityCollision(world, pos, part)) {
            return false;
        }

        TileMultipart t = getOrConvertTile(world, pos);
        if (t != null) {
            return t.canAddPart(part);
        } else if (!compatibilityAllows(world, pos.x, pos.y, pos.z)) {
            return false;
        }

        return replaceable(world, pos);
    }

    /** Returns if the block at pos is replaceable (air, vines etc). */
    public static boolean replaceable(World world, BlockCoord pos) {
        net.minecraft.block.Block block = world.getBlock(pos.x, pos.y, pos.z);
        return block.isAir(world, pos.x, pos.y, pos.z) || block.isReplaceable(world, pos.x, pos.y, pos.z);
    }

    /**
     * Adds a part to a block space. canPlacePart should always be called first. The addition of parts on the client is
     * handled internally.
     */
    public static TileMultipart addPart(World world, BlockCoord pos, TMultiPart part) {
        if (world.isRemote) {
            throw new AssertionError("assertion failed: Cannot add multi parts to a client tile.");
        }
        return MultipartGenerator$.MODULE$.addPart(world, pos, part);
    }

    /** Constructs this tile and its parts from a desc packet. */
    public static void handleDescPacket(World world, BlockCoord pos, PacketCustom packet) {
        int nparts = packet.readUByte();
        List<TMultiPart> parts = new ArrayList<>();
        for (int i = 0; i < nparts; i++) {
            TMultiPart part = MultiPartRegistry.readPart(packet);
            part.readDesc(packet);
            parts.add(part);
        }

        if (parts.isEmpty()) {
            return;
        }

        TileEntity t = world.getTileEntity(pos.x, pos.y, pos.z);
        TileMultipart tilemp = MultipartGenerator$.MODULE$.generateCompositeTile(t, toSeq(parts), true);
        if (tilemp != t) {
            world.setBlock(pos.x, pos.y, pos.z, MultipartProxy.block());
            MultipartGenerator.silentAddTile(world, pos, tilemp);
        }

        tilemp.loadParts(toSeq(parts));
        tilemp.notifyTileChange();
        tilemp.markRender();
    }

    /** Handles an update packet, addition, removal and otherwise. */
    public static void handlePacket(BlockCoord pos, World world, int i, PacketCustom packet) {
        if (i == 253) {
            TMultiPart part = MultiPartRegistry.readPart(packet);
            part.readDesc(packet);
            MultipartGenerator$.MODULE$.addPart(world, pos, part);
        } else if (i == 254) {
            // The reference resolves the tile separately for the receiver and the argument.
            TileMultipart receiver = TileCache.findTile(world, pos);
            TMultiPart removed = TileCache.findTile(world, pos).partList().apply(packet.readUByte());
            receiver.remPart_impl(removed);
        } else {
            TileCache.findTile(world, pos).partList().apply(i).read(packet);
        }
    }

    /** Creates this tile from an NBT tag. */
    public static TileMultipart createFromNBT(NBTTagCompound tag) {
        NBTTagList partList = tag.getTagList("parts", 10);
        List<TMultiPart> parts = new ArrayList<>();

        for (int i = 0; i < partList.tagCount(); i++) {
            NBTTagCompound partTag = partList.getCompoundTagAt(i);
            String partID = partTag.getString("id");
            TMultiPart part = MultiPartRegistry.loadPart(partID, partTag);
            if (part != null) {
                part.load(partTag);
                parts.add(part);
            }
        }

        if (parts.isEmpty()) {
            return null;
        }

        TileMultipart tmb = MultipartGenerator$.MODULE$.generateCompositeTile(null, toSeq(parts), false);
        tmb.readFromNBT(tag);
        tmb.loadParts(toSeq(parts));
        return tmb;
    }

    /** Drops an item around pos. */
    public static void dropItem(ItemStack stack, World world, Vector3 pos) {
        EntityItem item = new EntityItem(world, pos.x, pos.y, pos.z, stack);
        item.motionX = world.rand.nextGaussian() * 0.05;
        item.motionY = world.rand.nextGaussian() * 0.05 + 0.2;
        item.motionZ = world.rand.nextGaussian() * 0.05;
        item.delayBeforeCanPickup = 10;
        world.spawnEntityInWorld(item);
    }
}
