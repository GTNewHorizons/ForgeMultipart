package codechicken.microblock;

import net.minecraft.block.Block;

import codechicken.lib.render.CCRenderPipeline;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.ColourMultiplier;
import codechicken.lib.render.uv.UVTransformation;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.handler.MicroblockMod;

public final class MaterialRenderHelper$ {

    public static final MaterialRenderHelper$ MODULE$ = new MaterialRenderHelper$();

    private final ThreadLocal<ThreadState> threadState = ThreadLocal.withInitial(ThreadState::new);

    private MaterialRenderHelper$() {}

    private ThreadLocal<ThreadState> threadState() {
        return threadState;
    }

    public int pass() {
        return threadState().get().pass();
    }

    public void pass_$eq(int pass) {
        threadState().get().pass_$eq(pass);
    }

    public CCRenderPipeline.PipelineBuilder builder() {
        return threadState().get().builder();
    }

    public void builder_$eq(CCRenderPipeline.PipelineBuilder builder) {
        threadState().get().builder_$eq(builder);
    }

    public MaterialRenderHelper$ start(Vector3 pos, int pass, UVTransformation uvt) {
        pass_$eq(pass);
        builder_$eq(CCRenderState.instance().pipeline.builder());
        builder().add(pos.translation()).add(uvt);
        return this;
    }

    public MaterialRenderHelper$ blockColour(int colour) {
        builder().add(ColourMultiplier.instance(colour));
        return this;
    }

    public MaterialRenderHelper$ lighting() {
        if (pass() != -1) {
            builder().add(CCRenderState.instance().lightMatrix);
        }
        return this;
    }

    public MaterialRenderHelper$ blockAndMeta(Block block, int meta) {
        if (MicroblockMod.angelicaCompat() != null) {
            MicroblockMod.angelicaCompat().setShaderMaterialOverride(block, meta);
        }
        return this;
    }

    public void render() {
        builder().render();
        if (MicroblockMod.angelicaCompat() != null) {
            MicroblockMod.angelicaCompat().resetShaderMaterialOverride();
        }
    }
}
