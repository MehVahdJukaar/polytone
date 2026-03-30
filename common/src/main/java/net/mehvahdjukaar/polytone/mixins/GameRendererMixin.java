package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 500)
public abstract class GameRendererMixin {

    @Inject(method = "render", at = @At(value = "NEW",
            target = "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/render/state/GuiRenderState;II)Lnet/minecraft/client/gui/GuiGraphics;"))
    private void polytone$messWithGui(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        Polytone.LIGHTMAPS.setupForGUI(true);
        Polytone.OVERLAY_MODIFIERS.onStartRenderingOverlay();
    }

    @Inject(method = "render", at = @At(value = "TAIL"))
    private void polytone$resetGuiLightmap(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        Polytone.LIGHTMAPS.setupForGUI(false);
        Polytone.OVERLAY_MODIFIERS.onEndRenderingOverlay();
    }

    @Inject(method = "close", at = @At(value = "TAIL"))
    private void polytone$closeShaderStuff(CallbackInfo ci) {
        Polytone.POST_SHADERS.onClose();
    }

}
