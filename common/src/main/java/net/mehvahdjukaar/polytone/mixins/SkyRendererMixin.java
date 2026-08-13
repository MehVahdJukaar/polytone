package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// When the sky_depth_write config is enabled, swaps the sky-disc draw for a pipeline variant that writes depth
// (see PolytoneRenderTypes#SKY_DEPTH_WRITE_PIPELINE). This only affects the top sky disc (renderSkyDisc); the
// dark disc is left alone.
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {

    @ModifyExpressionValue(method = "renderSkyDisc",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/RenderPipelines;SKY:Lcom/mojang/blaze3d/pipeline/RenderPipeline;"))
    private RenderPipeline polytone$skyDepthWrite(RenderPipeline original) {
        if (Polytone.CONFIGS.skyDepthWrite.get()) {
            return PolytoneRenderTypes.SKY_DEPTH_WRITE_PIPELINE;
        }
        return original;
    }
}
