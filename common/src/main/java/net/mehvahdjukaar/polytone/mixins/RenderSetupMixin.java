package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderSetup.class)

public class RenderSetupMixin {

    @ModifyExpressionValue(method = "getTextures", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;getTextureView()Lcom/mojang/blaze3d/textures/GpuTextureView;"))
    public GpuTextureView polytone$onGetGuiLightTexture(GpuTextureView original) {
        if (Polytone.LIGHTMAPS.isGui()) {
            return Polytone.LIGHTMAPS.getGuiLightTexture().getTextureView();
        }
        return original;
    }
}