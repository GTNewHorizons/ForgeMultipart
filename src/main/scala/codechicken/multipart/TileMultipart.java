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

    /**
     * @deprecated Use {@link #jPartList()} for Java collection access. Retained as the virtual storage accessor for
     *             existing subclasses and binaries.
     */
    @Deprecated
    public Seq<TMultiPart> partList() {
        return partList;
    }

    /**
     * @deprecated Use {@link #setPartList(List)} for Java storage assignment. This legacy hook retains the exact
     *             supplied sequence, including mutable storage or null; existing overrides remain supported.
     */
    @Deprecated
    public void partList_$eq(Seq<TMultiPart> parts) {
        partList = parts;
    }

    /**
     * Assigns a shallow, immutable copy of the supplied list through the legacy {@link #partList_$eq(Seq)} hook.
     * Preserves order, duplicate entries and part identities. Later list edits do not affect the stored sequence.
     *
     * <p>
     * This is low-level storage assignment for reconstruction. It does not bind parts, rebuild trait caches, change
     * ticking or send notifications. Use {@link #loadPartList(Collection)} to load a correctly prepared composite tile;
     * use {@link #addPart(World, BlockCoord, TMultiPart)} / {@link #remPart(TMultiPart)} for normal world changes.
     * Existing setter overrides can customize storage. Internal writes still call the legacy hook directly.
     *
     * @param parts list to copy; null preserves the legacy unset-state sentinel, and null entries are not validated
     */
    public void setPartList(List<TMultiPart> parts) {
        partList_$eq(parts == null ? null : toSeq(parts));
    }

    /**
     * Internal FMP composite-transition hook: copies trait state, then rebinds the stored parts. Not a consumer entry
     * point.
     */
    public void from(TileMultipart that) {
        copyFrom(that);
        loadFrom(that);
    }

    /**
     * Internal FMP composite-transition hook; do not call directly from consumers.
     *
     * This method should be used for copying all the data from the fields in that container tile. This method will be
     * automatically generated on java tile traits with fields if it is not overridden.
     */
    public void copyFrom(TileMultipart that) {
        partList_$eq(that.partList());
        doesTick = that.doesTick;
    }

    /**
     * Internal FMP composite-transition hook: rebinds this tile's stored parts and restores ticking after state copy.
     * For consumer reconstruction, use {@link #loadPartList(Collection)} on a prepared composite instead.
     */
    public void loadFrom(TileMultipart that) {
        Iterator<TMultiPart> iterator = partList().iterator();
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

    /**
     * Returns an ordered Java view of the sequence returned by {@link #partList()} at this call. Includes detached
     * parts; replacing the tile's sequence does not update an existing view. Normally the sequence is immutable, but a
     * mutable sequence supplied through the legacy setter remains live through this view.
     *
     * <p>
     * Treat the view as read-only: list edits do not perform multipart binding, notifications or synchronization. Use
     * {@link #addPart(World, BlockCoord, TMultiPart)} and {@link #remPart(TMultiPart)} for world changes. Copy into an
     * {@link ArrayList} when independent list storage is needed; the parts themselves remain shared.
     *
     * @return a view of the captured sequence, with its original order and part identities
     */
    public List<TMultiPart> jPartList() {
        return JavaConversions.seqAsJavaList(partList());
    }

    /**
     * Visits parts in sequence order, skipping those whose {@link TMultiPart#tile()} is null when reached. Delegates
     * through the legacy {@link #operate(Function1)} hook, so existing overrides still control traversal.
     *
     * <p>
     * The default hook captures {@link #partList()} once. Normal additions publish a new sequence and are not visited
     * by the current call; parts detached or rebound to another tile by an earlier callback are skipped. Legacy mutable
     * sequences retain their own iterator behavior and must not be structurally edited during traversal. Each nested
     * call captures its own sequence. Callback exceptions propagate immediately, stopping the current traversal.
     *
     * <p>
     * Call on the game thread. Lifecycle callbacks continue to dispatch through {@code operate}, not through overrides
     * of this convenience method. With the default hook, a null callback fails only when a bound part is visited.
     *
     * @param consumer action to apply to each visited part
     */
    public void forEachPart(Consumer<TMultiPart> consumer) {
        operate(action(consumer));
    }

    @Override
    public boolean canUpdate() {
        return doesTick;
    }

    /**
     * Legacy traversal hook, also used by multipart lifecycle callbacks.
     *
     * @deprecated Call {@link #forEachPart(Consumer)} from Java. Existing overrides remain supported and intercept both
     *             that API and lifecycle callbacks; keep such overrides here while this compatibility hook is retained.
     */
    @Deprecated
    // Direct list traversal avoids measured iterator/wrapper allocations; the setter also accepts other Seq types.
    @SuppressWarnings("unchecked")
    public void operate(Function1<TMultiPart, BoxedUnit> f) {
        Seq<TMultiPart> current = partList();
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

    private void applyIfBound(Function1<TMultiPart, BoxedUnit> f, TMultiPart p) {
        // A replaced tile can still receive queued callbacks after its parts have moved.
        if (p.tile() == this) {
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

    /** Internal FMP validity update that bypasses multipart lifecycle callbacks. Not a consumer entry point. */
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
                Iterator<TMultiPart> iterator = partList().iterator();
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

    /**
     * Supported consumer API for local part-change notifications, used by ProjectRed. Calls
     * {@link TMultiPart#onPartChanged(TMultiPart)} through the retained {@link #operate(Function1)} hook. Its base
     * traversal captures the current list/order and visits only parts still bound to this tile at callback time. Parts
     * equal to {@code part} (using {@code part.equals(p)}) are excluded; null broadcasts to all eligible parts.
     * Callback failures propagate and stop traversal.
     *
     * This method does not mark the tile dirty, send updates or notify neighboring blocks/lighting. Keep the caller's
     * synchronization and external-notification policy, or use {@link #notifyPartChange(TMultiPart)} when world
     * notifications are needed as well.
     */
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
        if (partList().isEmpty()) {
            return 0;
        }
        int max = 0;
        Seq<TMultiPart> current = partList();
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
        Seq<TMultiPart> current = partList();
        if (current.isEmpty()) {
            // Matches the reference, where max on an empty view throws.
            throw new UnsupportedOperationException("empty.max");
        }
        float max = Float.NEGATIVE_INFINITY;
        Iterator<TMultiPart> iterator = current.iterator();
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
        Iterator<TMultiPart> iterator = partList().iterator();
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
        return compatibilityAllows(worldObj, xCoord, yCoord, zCoord) && !partList().contains(part)
                && occlusionTest(partList(), part);
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
        Iterator<TMultiPart> iterator = partList().iterator();
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

    /**
     * Tests a candidate against a shallow snapshot of the supplied parts through the legacy
     * {@link #occlusionTest(Seq, TMultiPart)} hook, including generated partial-occlusion overrides. Preserves
     * collection iteration order, duplicates and part identities; changes to the caller's collection during callbacks
     * do not change this query's sequence.
     *
     * <p>
     * The default hook calls each existing part's occlusion test first, then the candidate's reverse test, stopping
     * immediately on rejection or an exception. It includes detached parts and does not exclude the candidate itself.
     * Part objects remain shared, so callback changes to their state are visible. Existing overrides may add checks.
     *
     * <p>
     * This tests the supplied geometry, not full placement validity: it does not select tile capabilities, check
     * placement permissions or slot occupancy, bind parts or change stored parts. Use the appropriate composite tile
     * for the candidate's capabilities; use {@link #canAddPart(TMultiPart)} /
     * {@link #canReplacePart(TMultiPart, TMultiPart)} for normal tile checks. Those methods still dispatch directly
     * through the legacy hook.
     *
     * @param parts     parts to copy before dispatch; null fails before calling the legacy hook
     * @param candidate part to test; no eager null check, so the default hook accepts null against an empty collection
     * @return true when all applicable occlusion checks allow the candidate
     */
    public boolean testOcclusion(Collection<? extends TMultiPart> parts, TMultiPart candidate) {
        return occlusionTest(toSeq(new ArrayList<>(parts)), candidate);
    }

    /**
     * Returns true if parts do not occlude npart.
     *
     * @deprecated Call {@link #testOcclusion(Collection, TMultiPart)} from Java. Retained as the virtual hook for
     *             existing subclasses, generated traits and tile placement/replacement checks; its sequence behavior is
     *             unchanged.
     */
    @Deprecated
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
        return writeStream().writeByte(partList().indexOf(part));
    }

    private MCDataOutput writeStream() {
        return MultipartSPH.getTileStream(worldObj, new BlockCoord(this));
    }

    /**
     * Internal FMP placement lifecycle and synchronization hook. Consumers use
     * {@link #addPart(World, BlockCoord, TMultiPart)} so composite selection and world installation also run.
     */
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

    /** Internal FMP add-part packet writer; not a consumer entry point. Use the public placement API. */
    public void writeAddPart(TMultiPart part) {
        MCDataOutput stream = writeStream().writeByte(253);
        MultiPartRegistry.writePartID(stream, part);
        part.writeDesc(stream);
    }

    /**
     * Internal FMP storage/cache/binding step, without the complete placement lifecycle. Consumers use
     * {@link #addPart(World, BlockCoord, TMultiPart)} or {@link #loadPartList(Collection)} for prepared reconstruction.
     */
    public void addPart_do(TMultiPart part) {
        if (partList().size() >= 250) {
            throw new AssertionError(
                    "assertion failed: Tried to add more than 250 parts to the one tile. You're doing it wrong");
        }

        List<TMultiPart> next = mutablePartsSnapshot();
        next.add(part);
        partList_$eq(toSeq(next));
        bindPart(part);
        part.bind(this);

        if (!doesTick && part.doesTick()) {
            setTicking(true);
        }
    }

    /**
     * Supported advanced consumer API and trait override hook for adding a part to this tile's capability caches. The
     * base implementation is empty; generated traits supply cache behavior. Does not insert the part into the part
     * list, change {@link TMultiPart#tile()}, run placement callbacks or send notifications.
     *
     * OpenComputers uses this to refresh a changed slot mask after clearing that part's old slot entries. Rebinding
     * alone does not clear obsolete slots, and other traits may append cache entries on every call; this is not a
     * general-purpose idempotent refresh. Only use it with known capability/cache semantics. For placement use
     * {@link #addPart(World, BlockCoord, TMultiPart)}; for full reconstruction use {@link #loadPartList(Collection)}.
     */
    public void bindPart(TMultiPart part) {}

    /**
     * Refreshes the generated slot cache after an already stored part changes its {@link TSlottedPart#getSlotMask()}.
     * Generated slotted tiles remove every cached entry equal to {@code part}, then dispatch
     * {@link #bindPart(TMultiPart)} once to add its current slots. A tile without the slotted capability does nothing.
     *
     * <p>
     * This does not validate placement, insert or bind the part, change the stored list, send notifications, mark the
     * tile dirty/renderable or schedule updates. Call only after the part has validated its new shape and updated its
     * mask, on the owning world thread. The part normally remains stored and bound to this tile; no eager validation is
     * added. Other generated bind hooks also run, so callers must retain their existing cache-specific assumptions and
     * perform their own notifications afterward.
     */
    public void refreshPartSlots(TMultiPart part) {}

    /** Internal FMP placement callback for trait overrides; do not call directly from consumers. */
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

    /** Internal FMP removal/synchronization hook. Consumers use {@link #remPart(TMultiPart)} on the server. */
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
        int r = partList().indexOf(part);
        if (r < 0) {
            throw new IllegalArgumentException("Tried to remove a non-existant part");
        }

        // The reference read an unused removedLightValue here; the virtual call is observable, so keep it.
        part.getLightValue();
        part.preRemove();
        List<TMultiPart> current = mutablePartsSnapshot();
        current.removeIf(p -> p == null ? part == null : p.equals(part));
        partList_$eq(toSeq(current));

        if (sendPacket) {
            writeStream().writeByte(254).writeByte(r);
        }

        partRemoved(part, r);
        part.onRemoved();
        part.tile_$eq(null);

        if (partList().isEmpty()) {
            worldObj.setBlockToAir(xCoord, yCoord, zCoord);
        } else if (part.doesTick() && doesTick) {
            boolean ntick = false;
            Iterator<TMultiPart> iterator = partList().iterator();
            while (iterator.hasNext()) {
                ntick |= iterator.next().doesTick();
            }
            if (!ntick) {
                setTicking(false);
            }
        }
        return r;
    }

    /** Internal FMP cache-removal callback for trait overrides; do not call directly from consumers. */
    public void partRemoved(TMultiPart part, int p) {}

    /**
     * Loads parts into an already prepared composite tile through the legacy
     * {@link #loadParts(scala.collection.Iterable)} hook. Clears trait caches, then stores and binds each input part in
     * collection iteration order. The collection is viewed during the call, not copied in advance; do not structurally
     * modify it from callbacks.
     *
     * <p>
     * With a world, the default hook calls {@code onWorldJoin} on the client and then
     * {@link #notifyPartChange(TMultiPart)} with null on either side. Without a world it only loads/binds. It does not
     * generate capabilities, remove/unbind old parts, call {@code onAdded}, reset the existing ticking flag or send a
     * complete description packet. Use {@link MultipartHelper#createTileFromParts(Iterable)} when constructing a new
     * server tile from parts.
     *
     * <p>
     * Call on the game thread. Loading is not atomic: failures propagate with cleared or partially loaded state. The
     * default hook clears before reading the collection, so a null collection also fails after clearing. Existing
     * legacy overrides still control loading; internal reconstruction continues to call that hook directly.
     *
     * @param parts parts to load; iteration order, duplicates and callback failure behavior are retained
     */
    public void loadPartList(Collection<TMultiPart> parts) {
        loadParts(JavaConversions.iterableAsScalaIterable(parts));
    }

    /**
     * @deprecated Call {@link #loadPartList(Collection)} from Java. Retained for legacy overrides, compiled callers and
     *             exact Scala-parameter reflection; behavior is unchanged.
     */
    @Deprecated
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
        partList_$eq(emptyParts());
    }

    /** Writes the description of this tile, and all parts composing it, to packet. */
    public void writeDesc(MCDataOutput packet) {
        packet.writeByte(partList().size());
        Iterator<TMultiPart> iterator = partList().iterator();
        while (iterator.hasNext()) {
            TMultiPart part = iterator.next();
            MultiPartRegistry.writePartID(packet, part);
            part.writeDesc(packet);
        }
    }

    /** Perform a raytrace returning all intersecting parts sorted nearest to farthest. */
    public Iterable<ExtendedMOP> rayTraceAll(Vec3 start, Vec3 end) {
        List<ExtendedMOP> list = new ArrayList<>();
        Iterator<TMultiPart> iterator = partList().iterator();
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
        TMultiPart part = partList().apply(index);
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
        Iterator<TMultiPart> iterator = partList().iterator();
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
        return new ArrayList<>(JavaConversions.seqAsJavaList(partList()));
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

    /**
     * Returns FMP's global block-render registration ID. Initially -1; reading it does not initialize the client
     * renderer or allocate an ID. This is shared state, not a per-tile or persistent part ID.
     */
    public static int getRenderID() {
        return renderID();
    }

    /**
     * Assigns the global render ID, preserving all integer values including -1. Intended for controlled render setup;
     * this only changes the stored value and does not allocate an ID, register a handler or update Forge's mapping.
     * FMP's client renderer still performs its own allocation when first initialized. Use on the initialization/game
     * thread, not as a per-tile rendering control.
     */
    public static void setRenderID(int value) {
        renderID_$eq(value);
    }

    /** @deprecated Use {@link #getRenderID()}. Retained for existing static/companion callers. */
    @Deprecated
    public static int renderID() {
        return renderID;
    }

    /** @deprecated Use {@link #setRenderID(int)}. Retained with unchanged shared-state behavior. */
    @Deprecated
    public static void renderID_$eq(int value) {
        renderID = value;
    }

    /**
     * Gets an existing tile or converted placeholder, or null. Conversion does not install the tile in the world. Use
     * {@link #getOrConvertTileResult(World, BlockCoord)} when the conversion flag is also needed.
     */
    public static TileMultipart getOrConvertTile(World world, BlockCoord pos) {
        return getOrConvertTile2(world, pos)._1();
    }

    /**
     * Looks up an existing tile, otherwise asks registered block converters for a multipart representation. An existing
     * tile is returned by identity with {@link TileConversionResult#isConverted()} false. A successful conversion
     * returns a fresh, uninstalled placeholder with the flag true; no match returns a null tile and false.
     *
     * <p>
     * A placeholder uses the world's client/server side and has its coordinates, world and converted part bound. This
     * lookup does not replace the block, install the tile or invoke the part's placement/conversion callbacks. Repeated
     * lookups can construct different placeholders. Converter callbacks run normally and their exceptions propagate;
     * this is not a side-effect-free query. Use on the appropriate world thread after FMP initialization.
     *
     * <p>
     * For ordinary placement, use {@link #canPlacePart(World, BlockCoord, TMultiPart)} followed by
     * {@link #addPart(World, BlockCoord, TMultiPart)} and retain the tile returned by the addition. Do not install this
     * placeholder directly. Use {@link #getTile(World, BlockCoord)} if conversion should not be attempted.
     *
     * @return a non-null result recording this lookup; its tile reference may be null
     */
    public static TileConversionResult getOrConvertTileResult(World world, BlockCoord pos) {
        Tuple2<TileMultipart, Object> result = getOrConvertTile2(world, pos);
        return new TileConversionResult(result._1(), (Boolean) result._2());
    }

    /**
     * Gets a multipart tile instance at pos, converting if necessary. Note converted tiles are merely a structure
     * formality, they do not actually exist in world until they are required to by the addition of another multipart to
     * their space.
     *
     * @return the tile or null if there was none, and true if the tile is a result of a conversion
     * @deprecated Use {@link #getOrConvertTileResult(World, BlockCoord)} for a Java result with named accessors.
     */
    @Deprecated
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
