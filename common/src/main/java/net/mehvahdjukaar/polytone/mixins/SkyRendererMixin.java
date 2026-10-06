package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

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

    @ModifyArg(method = {"renderSun(FLcom/mojang/blaze3d/vertex/PoseStack;)V",
            "renderMoon(Lnet/minecraft/world/level/MoonPhase;FLcom/mojang/blaze3d/vertex/PoseStack;)V"},
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"))
    private RenderPipeline polytone$translucentSunAndMoon(RenderPipeline pipeline) {
        return Polytone.DIMENSION_MODIFIERS.translucentSunAndMoon() ? PolytoneRenderTypes.TRANSLUCENT_CELESTIAL_PIPELINE : pipeline;
    }
}
