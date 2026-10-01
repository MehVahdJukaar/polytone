package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

// When the sky_depth_write config is enabled, swaps the sky-disc draw for a pipeline variant that writes depth
// (see PolytoneRenderTypes#SKY_DEPTH_WRITE_PIPELINE). This only affects the top sky disc (renderSkyDisc); the
// dark disc is left alone.
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {

    @Shadow
    @Final
    private GpuBuffer endFlashBuffer;

    @ModifyExpressionValue(method = "renderSkyDisc",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/RenderPipelines;SKY:Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"))
    private RenderPipeline polytone$skyDepthWrite(RenderPipeline original) {
        if (Polytone.CONFIGS.skyDepthWrite.get()) {
            return PolytoneRenderTypes.SKY_DEPTH_WRITE_PIPELINE;
        }
        return original;
    }

    @ModifyExpressionValue(method = "drawCelestialBody",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/RenderPipelines;CELESTIAL:Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"))
    private RenderPipeline polytone$translucentSunAndMoon(RenderPipeline pipeline, @Local(argsOnly = true, ordinal = 1) GpuBuffer vertexBuffer) {
        if (vertexBuffer == this.endFlashBuffer) return pipeline;
        return Polytone.DIMENSION_MODIFIERS.translucentSunAndMoon() ? PolytoneRenderTypes.TRANSLUCENT_CELESTIAL_PIPELINE : pipeline;
    }
}
