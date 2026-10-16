package codechicken.multipart.examples;

import java.util.List;

import net.minecraft.util.MovingObjectPosition;

import codechicken.multipart.BlockMultipart;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

/** Compiling example for docs/api/HIT_PART.md. */
public final class HitPartExample {

    private HitPartExample() {}

    /** Returns the part a ray trace struck in this tile, or null if the hit does not select one of its parts. */
    public static TMultiPart struckPart(TileMultipart tile, MovingObjectPosition hit) {
        int index = BlockMultipart.hitPartIndex(hit);
        List<TMultiPart> parts = tile.jPartList();
        return index >= 0 && index < parts.size() ? parts.get(index) : null;
    }
}
