package codechicken.multipart.examples;

import java.util.ArrayList;
import java.util.List;

import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

/** Compiling examples for docs/api/PART_LOADING.md; these are reconstruction operations, not placement/removal. */
public final class PartLoadingExample {

    private PartLoadingExample() {}

    /** Captures the existing parts before clearing/rebuilding the prepared tile's caches and bindings. */
    public static void reloadParts(TileMultipart tile) {
        List<TMultiPart> parts = new ArrayList<>(tile.jPartList());
        tile.loadPartList(parts);
    }

    /**
     * Stages a copy of the list only; bindings, ticking, caches and notifications remain the caller's responsibility.
     */
    public static void stageParts(TileMultipart tile, List<TMultiPart> parts) {
        tile.setPartList(parts);
    }
}
