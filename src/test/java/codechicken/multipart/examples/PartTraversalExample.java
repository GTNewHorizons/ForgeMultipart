package codechicken.multipart.examples;

import java.util.ArrayList;
import java.util.List;

import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

/** Compiling examples for docs/api/PART_TRAVERSAL.md; call on the game thread. */
public final class PartTraversalExample {

    private PartTraversalExample() {}

    public static List<String> partTypes(TileMultipart tile) {
        List<String> types = new ArrayList<>();
        for (TMultiPart part : tile.jPartList()) {
            types.add(part.getType());
        }
        return types;
    }

    public static List<String> boundPartTypes(TileMultipart tile) {
        List<String> types = new ArrayList<>();
        tile.forEachPart(part -> types.add(part.getType()));
        return types;
    }
}
