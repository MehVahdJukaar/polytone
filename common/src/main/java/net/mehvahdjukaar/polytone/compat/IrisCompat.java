package net.mehvahdjukaar.polytone.compat;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;

public class IrisCompat {

    public static void init() {
        IrisApi.getInstance().assignPipeline(PolytoneRenderTypes.LEASH_PIPELINE, IrisProgram.TEXTURED);
        IrisApi.getInstance().assignPipeline(PolytoneRenderTypes.FISHING_ROD_PIPELINE, IrisProgram.TEXTURED);
        IrisApi.getInstance().assignPipeline(PolytoneRenderTypes.ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE, IrisProgram.PARTICLES_TRANSLUCENT);
        IrisApi.getInstance().assignPipeline(PolytoneRenderTypes.ADDITIVE_TRANSLUCENT_BLOCK_PIPELINE, IrisProgram.BLOCK_TRANSLUCENT);
    }

    public static boolean isIrisRenderOn() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline s) {
            return s.shouldOverrideShaders();
        }
        return false;
    }
}
