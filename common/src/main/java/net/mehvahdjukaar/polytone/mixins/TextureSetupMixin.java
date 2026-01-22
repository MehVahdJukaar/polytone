package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureSetup.class)
public class TextureSetupMixin {

    @ModifyExpressionValue(method = "singleTextureWithLightmap", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;getTextureView()Lcom/mojang/blaze3d/textures/GpuTextureView;"))
    private static GpuTextureView polytone$onGetGuiLightTexture(GpuTextureView original) {
        if (Polytone.LIGHTMAPS.isGui()) {
            return Polytone.LIGHTMAPS.getGuiLightTexture().getTextureView();
        }
        return original;
    }
}