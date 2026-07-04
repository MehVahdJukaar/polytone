package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.content.shaders.PostShadersManager;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies Polytone post-shader uniforms immediately before {@link EffectInstance#apply()} so they
 * are live on the GPU together with the {@code DiffuseSampler} binding that {@code PostPass.process}
 * sets in the same frame.
 */
@Mixin(PostPass.class)
public abstract class PostPassMixin {

    @Inject(method = "process",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/EffectInstance;apply()V",
                    shift = At.Shift.BEFORE))
    private void polytone$applyPostShaderUniforms(float partialTicks, CallbackInfo ci) {
        PostShadersManager.ActivePostPassFrame frame = PostShadersManager.ACTIVE_POST_PASS.get();
        if (frame == null) return;
        frame.effect().applyUniformsToEffect(((PostPass) (Object) this).getEffect(), frame);
    }
}
