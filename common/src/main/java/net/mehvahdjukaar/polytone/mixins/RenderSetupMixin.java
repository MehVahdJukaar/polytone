package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderSetup.class)
public class RenderSetupMixin {

    // 26.2: getTextures() -> prepareTextures(TextureManager, SamplerCache, GpuTextureView overlay, GpuTextureView lightmap).
    // The lightmap is now passed in as the 2nd GpuTextureView arg (ordinal 1) instead of fetched via GameRenderer.lightmap().
    @ModifyVariable(method = "prepareTextures", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public GpuTextureView polytone$onGetGuiLightTexture(GpuTextureView lightmapTexture) {
        if (Polytone.LIGHTMAPS.isGui()) {
            return Polytone.LIGHTMAPS.getGuiLightTexture().getTextureView();
        }
        return lightmapTexture;
    }
}
