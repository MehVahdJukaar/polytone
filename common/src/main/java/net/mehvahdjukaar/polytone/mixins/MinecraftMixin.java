package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.particle.PreviewRenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "addResourcePackLoadFailToast", at = @At("HEAD"))
    public void polytone$changeToast(Component message, CallbackInfo ci, @Local(argsOnly = true) LocalRef<Component> modifiableMessage) {
        if (Polytone.iMessedUp) {
            modifiableMessage.set(Component.translatable("toast.polytone.load_fail"));
            Polytone.iMessedUp = false;
        }
    }

    // While the particle editor preview draws offscreen, the vanilla particle feature renderer builds
    // its render pass from this target directly - send it to the preview's offscreen buffer instead of
    // the screen. Set only on the render thread for the duration of that one draw, so gameplay is untouched.
    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void polytone$redirectMainTargetForPreview(CallbackInfoReturnable<RenderTarget> cir) {
        if (!CompatHandler.NAUTILUS) return; // the preview that sets this only exists with the editor
        RenderTarget preview = PreviewRenderTarget.current();
        if (preview != null) cir.setReturnValue(preview);
    }

}
