package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShadowFeatureRenderer.class)
public abstract class ShadowFeatureRendererMixin {

    // colors.json "entity_shadows": false turns off the blob shadows under entities
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void polytone$cancelEntityShadow(CallbackInfo ci) {
        if (Polytone.COLORS.areEntityShadowsDisabled()) {
            ci.cancel();
        }
    }
}
