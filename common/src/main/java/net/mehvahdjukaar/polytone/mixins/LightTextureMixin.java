package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.lightmap.LightmapsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightTexture.class, priority = -201) //so we load before alex caves
public abstract class LightTextureMixin {

    @Shadow
    @Final
    private DynamicTexture lightTexture;

    @Shadow
    @Final
    private NativeImage lightPixels;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private float blockLightRedFlicker;

    @Shadow
    private boolean updateLightTexture;

    //needs to be same as alexcaves
    @Inject(
            method = "updateLightTexture(F)V",
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void polytone$modifyLightTexture(float partialTicks, CallbackInfo ci) {
        if (this.updateLightTexture) {
            ClientLevel clientlevel = this.minecraft.level;
            if (clientlevel != null) {
                this.minecraft.getProfiler().push("lightTex");
                if (Polytone.LIGHTMAPS.maybeModifyLightTexture((LightTexture) (Object) this, lightPixels, lightTexture,
                        minecraft, clientlevel, blockLightRedFlicker, partialTicks)) {
                    this.updateLightTexture = false;
                    ci.cancel();
                }
                this.minecraft.getProfiler().pop();
            }
        }
    }

    @Inject(method = "turnOnLightLayer", at = @At(value = "HEAD"), cancellable = true)
    public void polytone$useGuiLightmap(CallbackInfo ci) {
        if (Polytone.LIGHTMAPS.isGui()) {
            RenderSystem.setShaderTexture(2, LightmapsManager.GUI_LIGHTMAP);
            this.minecraft.getTextureManager().bindForSetup(LightmapsManager.GUI_LIGHTMAP);
            RenderSystem.texParameter(3553, 10241, 9729);
            RenderSystem.texParameter(3553, 10240, 9729);
            ci.cancel();
        }

    }
}
