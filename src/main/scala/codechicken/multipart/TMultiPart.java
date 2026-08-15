package codechicken.multipart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.raytracer.RayTracer;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class TMultiPart {

    /** Reference to the container TileMultipart instance. */
    private TileMultipart tile;

    public TileMultipart tile() {
        return tile;
    }

    public void tile_$eq(TileMultipart t) {
        tile = t;
    }

    /**
     * Legacy helper function for getting the tile entity (from when TileMultipart was a trait). Use tile() now.
     *
     * @deprecated use {@link #tile()}
     */
    @Deprecated
    public TileEntity getTile() {
        return tile;
    }

    /** Getter for tile.worldObj. */
    public World world() {
        return tile == null ? null : tile.getWorldObj();
    }

    /** Short getter for xCoord. */
    public int x() {
        return tile.xCoord;
    }

    /** Short getter for yCoord. */
    public int y() {
        return tile.yCoord;
    }

    /** Short getter for zCoord. */
    public int z() {
        return tile.zCoord;
    }

    /** The unique string identifier for this class of multipart. */
    public abstract String getType();

    /** Called when the container tile instance is changed to update reference. */
    public void bind(TileMultipart t) {
        tile = t;
    }

    /** Perform an occlusion test to determine whether this and npart can 'fit' in this block space. */
    public boolean occlusionTest(TMultiPart npart) {
        return true;
    }

    /**
     * Return a list of entity collision boxes. Note all Cuboid6's returned by methods in TMultiPart should be within
     * (0,0,0) to (1,1,1).
     */
    public Iterable<Cuboid6> getCollisionBoxes() {
        return Collections.emptyList();
    }

    /**
     * Perform a raytrace of this part. The default implementation does a Cuboid6 ray trace on bounding boxes returned
     * from getSubParts. This should only be overridden if you need special ray-tracing capabilities such as triangular
     * faces. The returned ExtendedMOP will be passed to methods such as 'activate' so it is recommended to use the data
     * field to indicate information about the hit area.
     */
    public ExtendedMOP collisionRayTrace(Vec3 start, Vec3 end) {
        Vector3 offset = new Vector3(x(), y(), z());
        List<IndexedCuboid6> boxes = new ArrayList<>();
        for (IndexedCuboid6 c : getSubParts()) {
            boxes.add(new IndexedCuboid6(c.data, c.copy().add(offset)));
        }
        return (ExtendedMOP) RayTracer.instance().rayTraceCuboids(
                new Vector3(start),
                new Vector3(end),
                boxes,
                new BlockCoord(x(), y(), z()),
                tile.getBlockType());
    }

    /**
     * For the default collisionRayTrace implementation, returns a list of indexed bounding boxes. The data field of
     * ExtendedMOP will be set to the index of the cuboid the raytrace hit.
     */
    public Iterable<IndexedCuboid6> getSubParts() {
        return Collections.emptyList();
    }

    /** Return a list of items that should be dropped when this part is destroyed. */
    public Iterable<ItemStack> getDrops() {
        return Collections.emptyList();
    }

    /**
     * Return a value indicating how hard this part is to break.
     *
     * @param hit An instance of ExtendedMOP from collisionRayTrace
     */
    public float getStrength(MovingObjectPosition hit, EntityPlayer player) {
        return 1;
    }

    /**
     * Harvest this part, removing it from the container tile and dropping items if necessary.
     *
     * @param hit    An instance of ExtendedMOP from collisionRayTrace
     * @param player The player harvesting the part
     */
    public void harvest(MovingObjectPosition hit, EntityPlayer player) {
        if (!player.capabilities.isCreativeMode) {
            tile.dropItems(getDrops());
        }
        tile.remPart(this);
    }

    /** The light level emitted by this part. */
    public int getLightValue() {
        return 0;
    }

    /** If any part returns true for this, torches can be placed. Vanilla hacks... */
    public boolean canPlaceTorchOnTop() {
        return false;
    }

    /**
     * Explosion resistance of the host tile is the maximum explosion resistance of the contained parts.
     *
     * @param entity The entity responsible for this explosion
     * @return The resistance of this part to the explosion
     */
    public float explosionResistance(Entity entity) {
        return 0f;
    }

    /**
     * Add particles and other effects when a player is mining this part.
     *
     * @param hit An instance of ExtendedMOP from collisionRayTrace
     */
    @SideOnly(Side.CLIENT)
    public void addHitEffects(MovingObjectPosition hit, EffectRenderer effectRenderer) {}

    /**
     * Add particles and other effects when a player broke this part.
     *
     * @param hit An instance of ExtendedMOP from collisionRayTrace
     */
    @SideOnly(Side.CLIENT)
    public void addDestroyEffects(MovingObjectPosition hit, EffectRenderer effectRenderer) {
        addDestroyEffects(effectRenderer);
    }

    /** @deprecated use {@link #addDestroyEffects(MovingObjectPosition, EffectRenderer)} */
    @SideOnly(Side.CLIENT)
    @Deprecated
    public void addDestroyEffects(EffectRenderer effectRenderer) {}

    /**
     * Render the static, unmoving faces of this part into the world renderer. The Tessellator is already drawing.
     *
     * @param pass The render pass, 1 or 0
     * @return true if vertices were added to the tessellator
     */
    @SideOnly(Side.CLIENT)
    public boolean renderStatic(Vector3 pos, int pass) {
        return false;
    }

    /**
     * Render the dynamic, changing faces of this part and other gfx as in a TESR. The Tessellator will need to be
     * started if it is to be used.
     *
     * @param pos   The position of this block space relative to the renderer, same as x, y, z passed to TESR
     * @param frame The partial interpolation frame value for animations between ticks
     * @param pass  The render pass, 1 or 0
     */
    @SideOnly(Side.CLIENT)
    public void renderDynamic(Vector3 pos, float frame, int pass) {}

    /** Draw the breaking overlay for this part. The overrideIcon in RenderBlocks will be set to the fracture icon. */
    @SideOnly(Side.CLIENT)
    public void drawBreaking(RenderBlocks renderBlocks) {}

    /**
     * Override the drawing of the selection box around this part.
     *
     * @param hit An instance of ExtendedMOP from collisionRayTrace
     * @return true if highlight rendering was overridden
     */
    @SideOnly(Side.CLIENT)
    public boolean drawHighlight(MovingObjectPosition hit, EntityPlayer player, float frame) {
        return false;
    }

    /**
     * @return A Cuboid6 bounding the render of this part for frustum culling. The bounds are relative to the tile
     *         coordinates.
     */
    public Cuboid6 getRenderBounds() {
        return Cuboid6.full;
    }

    /**
     * Write all the data required to describe a client version of this part to the packet. Called serverside, when a
     * client loads this part for the first time.
     */
    public void writeDesc(MCDataOutput packet) {}

    /**
     * Fill out this part with the description information contained in packet. Will be exactly as written from
     * writeDesc. Called clientside when a client loads this part for the first time.
     */
    public void readDesc(MCDataInput packet) {}

    /** Save part to NBT (only called serverside). */
    public void save(NBTTagCompound tag) {}

    /** Load part from NBT (only called serverside). */
    public void load(NBTTagCompound tag) {}

    /**
     * Gets a MCDataOutput instance for writing update data to clients with this part loaded. The write stream functions
     * as a buffer which is flushed in a compressed databurst packet at the end of the tick.
     */
    public MCDataOutput getWriteStream() {
        return tile.getWriteStream(this);
    }

    /**
     * Read and operate on data written to getWriteStream. Ensure all data this part wrote is read even if it's not
     * going to be used. The default implementation assumes a call to sendDescUpdate as the only use of getWriteStream.
     */
    public void read(MCDataInput packet) {
        readDesc(packet);
        tile.markRender();
    }

    /**
     * Quick and easy method to re-describe the whole part on the client. This will call read on the client which calls
     * readDesc unless overriden. Incremental changes should be sent rather than the whole description packet if
     * possible.
     */
    public void sendDescUpdate() {
        if (tile != null) {
            writeDesc(getWriteStream());
        }
    }

    /**
     * Called when a part is added or removed from this block space. The part parameter may be null if several things
     * have changed.
     */
    public void onPartChanged(TMultiPart part) {}

    /** Called when a neighbor block changed. */
    public void onNeighborChanged() {}

    /** Called when this part is added to the block space. */
    public void onAdded() {
        onWorldJoin();
    }

    /** Called when this part is removed from the block space. */
    public void onRemoved() {
        onWorldSeparate();
    }

    /** Called when the containing chunk is loaded on the server. */
    public void onChunkLoad() {
        onWorldJoin();
    }

    /** Called when the containing chunk is unloaded on the server. */
    public void onChunkUnload() {
        onWorldSeparate();
    }

    /**
     * Called when this part separates from the world (due to removal, chunk unload or other). Use this to sync with
     * external data structures. Called on both client and server.
     */
    public void onWorldSeparate() {}

    /**
     * Called when this part joins the world (due to placement, chunkload or frame move etc). Use this to sync with
     * external data structures. Called on both client and server.
     */
    public void onWorldJoin() {}

    /** Called when this part is converted from a normal block/tile (only applicable if a converter is registered). */
    public void onConverted() {
        onAdded();
    }

    /**
     * Called when this part is converted from a normal block/tile (only applicable if a converter has been registered)
     * before the original tile has been replaced. Use this to clear out things like inventory from the old tile.
     */
    public void invalidateConvertedTile() {}

    /** Called when this part has been moved without a save/load. */
    public void onMoved() {
        onWorldJoin();
    }

    /** Called just before this part is actually removed from the container tile. */
    public void preRemove() {}

    /**
     * Return whether this part needs update ticks. This will only be called on addition/removal so it should be a
     * constant for this instance.
     */
    public boolean doesTick() {
        return true;
    }

    /**
     * Return whether this part needs to be included in the dynamic render loop (renderDynamic). Default is false.
     * Override this to true for parts like lights that need per-frame rendering but no logic updates.
     */
    public boolean shouldRenderDynamic() {
        return false;
    }

    /** Called once per world tick. This will be called even if doesTick returns false if another part needs ticks. */
    public void update() {}

    /** Called when a scheduled tick is executed. */
    public void scheduledTick() {}

    /**
     * Sets a scheduledTick callback for this part ticks in the future. This is a world time value, so if the chunk is
     * unloaded and reloaded some time later, the tick may fire immediately.
     */
    public void scheduleTick(int ticks) {
        TickScheduler.scheduleTick(this, ticks);
    }

    /** Return the itemstack for the middle click pick-block function. */
    public ItemStack pickItem(MovingObjectPosition hit) {
        return null;
    }

    /**
     * Called on block right click. item is the player's held item. This should not modify the part client side. If the
     * client call returns false, the server will not call this function.
     *
     * @param hit An instance of ExtendedMOP from collisionRayTrace
     */
    public boolean activate(EntityPlayer player, MovingObjectPosition hit, ItemStack item) {
        return false;
    }

    /**
     * Called on block left click. item is the player's held item.
     *
     * @param hit An instance of ExtendedMOP from collisionRayTrace
     */
    public void click(EntityPlayer player, MovingObjectPosition hit, ItemStack item) {}

    /** Called when an entity is within this block space. May not actually collide with this part. */
    public void onEntityCollision(Entity entity) {}
}
