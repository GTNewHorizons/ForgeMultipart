package codechicken.multipart;

import java.util.BitSet;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import codechicken.lib.vec.BlockCoord;
import scala.collection.JavaConversions;

/**
 * Manages dynamic construction of multipart container tiles. Register a mixin against a part's marker interface to add
 * tile-level logic or interfaces whenever that part is present. Generated classes can be inspected with the ASM debug
 * configuration option.
 */
public final class MultipartGenerator {

    private MultipartGenerator() {}

    /**
     * Selects a composite tile with exactly the capabilities required by the supplied parts and side. Reuses a
     * registered multipart tile only when its trait set matches exactly; otherwise returns a fresh empty tile. A null
     * or non-multipart candidate is allowed. An unregistered multipart subclass throws NoSuchElementException.
     *
     * <p>
     * This is generation only: no parts are loaded/bound, no world or position is assigned, no state is copied, no
     * previous tile is invalidated and no tile is installed in a world. A reused tile retains all its existing state.
     * The caller prepares world/position or NBT state before {@link TileMultipart#loadPartList(java.util.Collection)}
     * and owns any subsequent installation and notifications. For ordinary placement use
     * {@link TileMultipart#addPart(World, BlockCoord, TMultiPart)} instead.
     *
     * <p>
     * Reads the iterable once per call, including reuse, without a snapshot. Do not mutate it during generation; retain
     * a repeatable collection for later loading. Null inputs/elements and iterator failures propagate through the
     * existing generator, without rollback of its scratch state or caches. Call on the initialization/game thread after
     * trait registration. Client tile selection does not convert server parts into client part variants.
     *
     * @param tile   candidate for exact reuse, or null for a fresh tile
     * @param parts  parts whose capabilities select the tile; use these same parts for subsequent loading
     * @param client true for a client tile, false for a server tile
     */
    public static TileMultipart generateCompositeTile(TileEntity tile, Iterable<TMultiPart> parts, boolean client) {
        return MultipartGenerator$.MODULE$
                .generateCompositeTile(tile, JavaConversions.iterableAsScalaIterable(parts), client);
    }

    public static BitSet getBitSet() {
        return MultipartGenerator$.MODULE$.getBitSet();
    }

    public static BitSet freshBitSet() {
        return MultipartGenerator$.MODULE$.freshBitSet();
    }

    /** Adds a tile without notifying neighbouring blocks or adding it to the tick list. */
    public static void silentAddTile(World world, BlockCoord pos, TileEntity tile) {
        MultipartGenerator$.MODULE$.silentAddTile(world, pos, tile);
    }

    /**
     * Registers a marker interface implemented by parts with the same generated tile trait on both sides. Names may use
     * dots or slashes. Call during common mod initialization, before FMP first inspects an implementing part class.
     *
     * <p>
     * For a Java source trait, pass its binary name as a string instead of loading its class. The trait is a top-level
     * class extending {@link TileMultipart}, possibly abstract, with a no-argument constructor; FMP rewrites it into a
     * runtime interface. Consumer-callable methods belong on a separate ordinary interface implemented by the trait. Do
     * not instantiate or invoke the raw trait class from consumer code.
     *
     * <p>
     * Registration only associates the marker with generated behavior. It does not construct a tile, load or bind
     * parts, or register a part factory. Repeating a marker registration for a side logs an error and retains the first
     * mapping. See {@code docs/api/CUSTOM_TILE_TRAITS.md} for the compiling pattern and transformer constraints.
     *
     * @param marker binary name of the ordinary interface implemented by requesting parts
     * @param trait  binary name of the generated tile trait
     */
    public static void registerTrait(String marker, String trait) {
        MultipartGenerator$.MODULE$.registerTrait(marker, trait);
    }

    /**
     * Registers side-specific generated tile traits for a part marker. This has the same timing, class-loading and
     * trait authoring contract as {@link #registerTrait(String, String)}. Either trait may be null to omit the
     * capability from that side; the argument order is client, then server. Register parent Java traits before child
     * traits.
     *
     * @param marker      binary name of the ordinary interface implemented by requesting parts
     * @param clientTrait client trait binary name, or null for none
     * @param serverTrait server trait binary name, or null for none
     */
    public static void registerTrait(String marker, String clientTrait, String serverTrait) {
        MultipartGenerator$.MODULE$.registerTrait(marker, clientTrait, serverTrait);
    }

    public static void registerPassThroughInterface(String name) {
        MultipartGenerator$.MODULE$.registerPassThroughInterface(name);
    }

    /**
     * Adds the interface to the container and forwards its methods to its single implementing part. A second part
     * implementing the same interface is rejected by the generated trait's occlusion check.
     */
    public static void registerPassThroughInterface(String name, boolean client, boolean server) {
        MultipartGenerator$.MODULE$.registerPassThroughInterface(name, client, server);
    }
}
