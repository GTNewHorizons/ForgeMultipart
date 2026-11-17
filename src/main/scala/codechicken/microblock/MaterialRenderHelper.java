package codechicken.microblock;

import net.minecraft.block.Block;

import codechicken.lib.render.CCRenderPipeline;
import codechicken.lib.render.uv.UVTransformation;
import codechicken.lib.vec.Vector3;

public final class MaterialRenderHelper {

    private MaterialRenderHelper() {}

    public static int pass() {
        return MaterialRenderHelper$.MODULE$.pass();
    }

    public static void pass_$eq(int pass) {
        MaterialRenderHelper$.MODULE$.pass_$eq(pass);
    }

    public static CCRenderPipeline.PipelineBuilder builder() {
        return MaterialRenderHelper$.MODULE$.builder();
    }

    public static void builder_$eq(CCRenderPipeline.PipelineBuilder builder) {
        MaterialRenderHelper$.MODULE$.builder_$eq(builder);
    }

    public static MaterialRenderHelper$ start(Vector3 pos, int pass, UVTransformation uvt) {
        return MaterialRenderHelper$.MODULE$.start(pos, pass, uvt);
    }

    public static MaterialRenderHelper$ blockColour(int colour) {
        return MaterialRenderHelper$.MODULE$.blockColour(colour);
    }

    public static MaterialRenderHelper$ lighting() {
        return MaterialRenderHelper$.MODULE$.lighting();
    }

    public static MaterialRenderHelper$ blockAndMeta(Block block, int meta) {
        return MaterialRenderHelper$.MODULE$.blockAndMeta(block, meta);
    }

    public static void render() {
        MaterialRenderHelper$.MODULE$.render();
    }
}
