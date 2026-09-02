package codechicken.multipart;

import java.util.BitSet;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import codechicken.lib.vec.BlockCoord;

/**
 * Manages dynamic construction of multipart container tiles. Register a mixin against a part's marker interface to add
 * tile-level logic or interfaces whenever that part is present. Generated classes can be inspected with the ASM debug
 * configuration option.
 */
public final class MultipartGenerator {

    private MultipartGenerator() {}

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

    /** Registers the same tile trait for both sides. */
    public static void registerTrait(String marker, String trait) {
        MultipartGenerator$.MODULE$.registerTrait(marker, trait);
    }

    /** Registers side-specific tile traits; either trait may be null to exclude that side. */
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
