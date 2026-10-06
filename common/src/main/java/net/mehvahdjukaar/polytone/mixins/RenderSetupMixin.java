package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderSetup.class)
public class RenderSetupMixin {

    //ordinal 1 is the lightmap
    @ModifyVariable(method = "prepareTextures", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    public GpuTextureView polytone$onGetGuiLightTexture(GpuTextureView lightmapTexture) {
        if (Polytone.LIGHTMAPS.isGui()) {
            return Polytone.LIGHTMAPS.getGuiLightTexture().getTextureView();
        }
        return lightmapTexture;
    }
}
