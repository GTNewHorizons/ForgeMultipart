package codechicken.microblock;

import codechicken.lib.render.CCRenderPipeline;

public class ThreadState {

    private int pass;
    private CCRenderPipeline.PipelineBuilder builder;

    public int pass() {
        return pass;
    }

    public void pass_$eq(int pass) {
        this.pass = pass;
    }

    public CCRenderPipeline.PipelineBuilder builder() {
        return builder;
    }

    public void builder_$eq(CCRenderPipeline.PipelineBuilder builder) {
        this.builder = builder;
    }
}
