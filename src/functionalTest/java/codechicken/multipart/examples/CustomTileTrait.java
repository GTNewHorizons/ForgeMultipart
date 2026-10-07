package codechicken.multipart.examples;

import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

/** Register by name before loading this Java class; FMP transforms it into a runtime trait interface. */
public abstract class CustomTileTrait extends TileMultipart implements CustomTileTraitCapability {

    @Override
    public int totalContribution() {
        return CustomTileTraitAccess.totalContribution(this);
    }
}

final class CustomTileTraitAccess {

    private CustomTileTraitAccess() {}

    static int totalContribution(Object trait) {
        int total = 0;
        for (TMultiPart part : ((TileMultipart) trait).jPartList()) {
            if (part instanceof CustomTileTraitPart) {
                total += ((CustomTileTraitPart) part).tileContribution();
            }
        }
        return total;
    }
}
