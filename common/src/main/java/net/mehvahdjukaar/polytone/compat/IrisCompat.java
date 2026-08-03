package net.mehvahdjukaar.polytone.compat;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;

public class IrisCompat {

    public static void init() {
        //TEXTURED -> glowing
        //Particles -> red green overlay
        //BASIC -> glowing
        //BLOCK works??
        IrisApi.getInstance().assignPipeline(PolytoneRenderTypes.LEASH_PIPELINE, IrisProgram.BLOCK);
        IrisApi.getInstance().assignPipeline(PolytoneRenderTypes.ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE, IrisProgram.PARTICLES_TRANSLUCENT);
        // no BLOCK_TRANSLUCENT assignment: 26.2 dropped our additive moving-block pipeline - model
        // particles now go through vanilla's submit path, which Iris already handles.
    }


    public static boolean isIrisRenderOn() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline s) {
            return s.shouldOverrideShaders();
        }
        return false;
    }
}
