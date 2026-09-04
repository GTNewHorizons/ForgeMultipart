package codechicken.multipart.mixin;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import cpw.mods.fml.common.Loader;

import javax.annotation.Nonnull;

public enum Mixins implements IMixins 
{
    RENDER_GLOBAL_MIXIN(new MixinBuilder("Redirects part block break renders to ISBRHModel")
            .addClientMixins("MixinRenderGlobal")
            .addRequiredMod(TargetedMod.GTNHLIB)
            .setPhase(Phase.EARLY));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return this.builder;
    }
}