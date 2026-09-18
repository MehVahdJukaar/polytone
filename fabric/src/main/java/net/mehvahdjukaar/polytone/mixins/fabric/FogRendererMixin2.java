package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.fluid.FluidPropertyModifier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin2 {

    @Inject(method = "setupFog", at = @At(value = "TAIL"))
    private static void polytone$modifyFogShape(Camera camera, FogRenderer.FogMode fogMode,
                                                float farPlaneDistance, boolean shouldCreateFog,
                                                float partialTick, CallbackInfo ci, @Local FogType fogType) {
        if (fogMode == FogRenderer.FogMode.FOG_TERRAIN && fogType == FogType.NONE) {
            var newFog = Polytone.BIOME_MODIFIERS.modifyFogParameters(
                    RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd());
            if (newFog != null) {
                RenderSystem.setShaderFogStart(newFog.x);
                RenderSystem.setShaderFogEnd(newFog.y);
            }

        }
        polytone$modifyFluidFogShape(camera);
    }

    private static void polytone$modifyFluidFogShape(Camera camera) {
        if (!Polytone.FLUID_MODIFIERS.hasAnyModifier()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        FluidState fluid = level.getFluidState(camera.getBlockPosition());
        if (fluid.isEmpty()) return;
        FluidPropertyModifier mod = Polytone.FLUID_MODIFIERS.getModifierOrVariant(fluid.getType());
        if (mod == null || !mod.hasFogShape() || !FluidPropertyModifier.isCameraSubmerged(camera, level, fluid)) return;
        mod.modifyFogShape(camera, level);
    }

}
