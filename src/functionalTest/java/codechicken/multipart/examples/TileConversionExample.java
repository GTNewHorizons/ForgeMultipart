package codechicken.multipart.examples;

import net.minecraft.world.World;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.TileConversionResult;
import codechicken.multipart.TileMultipart;

/** Forge-tested compiling example for docs/api/TILE_CONVERSION.md. */
public final class TileConversionExample {

    private TileConversionExample() {}

    /** Returns only a newly converted placeholder, for inspection without installing it. */
    public static TileMultipart findConvertedPlaceholder(World world, BlockCoord pos) {
        TileConversionResult result = TileMultipart.getOrConvertTileResult(world, pos);
        return result.isConverted() ? result.getTile() : null;
    }
}
