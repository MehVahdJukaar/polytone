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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LightTexture.class)
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

    @Inject(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyDarken(F)F",
            shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true)
    public void polytone$modifyLightTexture(float partialTicks, CallbackInfo ci, ClientLevel clientLevel) {
        if (Polytone.LIGHTMAPS.maybeModifyLightTexture((LightTexture) (Object) this, lightPixels, lightTexture,
                minecraft, clientLevel, blockLightRedFlicker, partialTicks)) {
            ci.cancel();
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
