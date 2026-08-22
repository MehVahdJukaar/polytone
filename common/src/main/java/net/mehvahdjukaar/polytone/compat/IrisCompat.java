package net.mehvahdjukaar.polytone.compat;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.vertices.ImmediateState;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.renderer.RenderPipelines;

public class IrisCompat {

    public static void init() {
        // TODO: leashes need their own pipeline under Iris. Copying vanilla TEXT gets us a GLYPH program that
        // doesn't shade leashes right, so for now we just fall back to vanilla leashes while shaders are on.
        IrisPipelines.copyPipeline(RenderPipelines.TEXT, PolytoneRenderTypes.LEASH_PIPELINE);
        IrisPipelines.copyPipeline(RenderPipelines.TRANSLUCENT_PARTICLE, PolytoneRenderTypes.ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE);
        IrisPipelines.copyPipeline(RenderPipelines.SKY, PolytoneRenderTypes.SKY_DEPTH_WRITE_PIPELINE);
    }


    public static void setShaderOverrideBypass(boolean bypass) {
        ImmediateState.bypass = bypass;
    }

    public static boolean isIrisRenderOn() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline s) {
            return s.shouldOverrideShaders();
        }
        return false;
    }
}
