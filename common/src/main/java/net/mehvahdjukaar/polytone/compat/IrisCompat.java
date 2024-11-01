package net.mehvahdjukaar.polytone;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;

public class IrisCompat {
    public IrisCompat() {
    }

    public static boolean isIrisRenderOn() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline s) {
            return s.shouldOverrideShaders();
        }
        return false;
    }
}
