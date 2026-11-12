package codechicken.multipart.examples;

import java.util.List;

import net.minecraft.world.World;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultipartGenerator;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

/** Forge-tested compiling example for docs/api/COMPOSITE_GENERATION.md. */
public final class CompositeGenerationExample {

    private CompositeGenerationExample() {}

    /**
     * Prepares and loads a fresh tile without installing it. Parts must already be loaded and appropriate for the
     * requested side. A supplied world must agree with that side; null is allowed for worldless staging.
     */
    public static TileMultipart createUninstalledTile(World world, BlockCoord pos, List<TMultiPart> parts,
            boolean client) {
        TileMultipart tile = MultipartGenerator.generateCompositeTile(null, parts, client);
        tile.xCoord = pos.x;
        tile.yCoord = pos.y;
        tile.zCoord = pos.z;
        tile.setWorldObj(world);
        tile.loadPartList(parts);
        return tile;
    }
}
