package codechicken.multipart.mixin;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

public enum TargetedMod implements ITargetMod {

    GTNHLIB("GTNHLib Core", "gtnhlib");

    private final TargetModBuilder builder;

    TargetedMod(String modId) {
        this(null, modId, null);
    }

    TargetedMod(String coreModClass, String modId) {
        this(coreModClass, modId, null);
    }

    TargetedMod(String coreModClass, String modId, String targetClass) {
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass).setModId(modId).setTargetClass(targetClass);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
