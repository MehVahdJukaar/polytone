package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.FogManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin2 {

    @Inject(method = "setupFog", at = @At(value = "TAIL"))
    private static void polytone$modifyFogShape(Camera camera, FogRenderer.FogMode fogMode,
                                                float farPlaneDistance, boolean shouldCreateFog,
                                                float partialTick, CallbackInfo ci, @Local FogType fogType) {
        if (fogMode == FogRenderer.FogMode.FOG_TERRAIN && fogType == FogType.NONE) {
            FogManager.FogState newFog = FogManager.modifyBiomeFog(
                    RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd());
            if (newFog != null) {
                RenderSystem.setShaderFogStart(newFog.start());
                RenderSystem.setShaderFogEnd(newFog.end());
            }

        }
        if (fogMode == FogRenderer.FogMode.FOG_TERRAIN && (fogType == FogType.WATER || fogType == FogType.LAVA)) {
            FogManager.FogState newFog = FogManager.modifyFluidFog(
                    RenderSystem.getShaderFogStart(),  RenderSystem.getShaderFogEnd(), null, null);
            if (newFog != null) {
                RenderSystem.setShaderFogStart(newFog.start());
                RenderSystem.setShaderFogEnd(newFog.end());
            }
        }
    }



}
