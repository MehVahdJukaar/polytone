package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FeatureRenderDispatcher.class)
public abstract class ShadowFeatureRendererMixin {

    //wraps since sodium cancels renderTranslucent at head
    @WrapWithCondition(method = "renderTranslucentFeatures", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ShadowFeatureRenderer;renderTranslucent(Lnet/minecraft/client/renderer/SubmitNodeCollection;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"))
    private boolean polytone$cancelEntityShadow(ShadowFeatureRenderer renderer, SubmitNodeCollection collection, MultiBufferSource.BufferSource bufferSource) {
        return !Polytone.COLORS.areEntityShadowsDisabled();
    }
}
