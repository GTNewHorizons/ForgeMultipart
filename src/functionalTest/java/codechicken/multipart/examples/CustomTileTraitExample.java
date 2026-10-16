package codechicken.multipart.examples;

import codechicken.multipart.MultipartGenerator;
import codechicken.multipart.TileMultipart;

/** Compiling registration and access example for a consumer-owned generated tile trait. */
public final class CustomTileTraitExample {

    private static final String TRAIT = "codechicken.multipart.examples.CustomTileTrait";

    private CustomTileTraitExample() {}

    /** Call once during mod initialization, before FMP inspects any implementing part class. */
    public static void register() {
        MultipartGenerator.registerTrait(CustomTileTraitPart.class.getName(), TRAIT);
    }

    /** Read through the stable capability interface; tiles without the custom trait contribute zero. */
    public static int totalContribution(TileMultipart tile) {
        return tile instanceof CustomTileTraitCapability ? ((CustomTileTraitCapability) tile).totalContribution() : 0;
    }
}
