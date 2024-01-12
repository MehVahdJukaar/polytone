package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.platform.NativeImage;
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
public class LightTextureMixin {

    @Shadow
    @Final
    private DynamicTexture lightTexture;

    @Shadow
    @Final
    private NativeImage lightPixels;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow private float blockLightRedFlicker;

    @Inject(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyDarken(F)F",
            shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true)
    public void polytone$modifyLightTexture(float partialTicks, CallbackInfo ci, ClientLevel clientLevel) {
        if (LightmapsManager.maybeModifyLightTexture((LightTexture) (Object) this, lightPixels, lightTexture,
                minecraft, clientLevel, blockLightRedFlicker, partialTicks)) {
            ci.cancel();
        }
    }
}
