package codechicken.microblock.compat;

import com.gtnewhorizons.postea.api.ItemStackReplacementManager;

public class PosteaCompat {

    public static void registerTransformers() {
        ItemStackReplacementManager
                .addTransformationHandler("ForgeMicroblock:microblock", new ItemMicroPartTransformer());
    }
}
