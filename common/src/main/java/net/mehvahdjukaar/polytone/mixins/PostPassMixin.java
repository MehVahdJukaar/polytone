package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.content.shaders.PostShadersManager;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// uniforms must go live together with the DiffuseSampler binding PostPass.process sets that frame
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
