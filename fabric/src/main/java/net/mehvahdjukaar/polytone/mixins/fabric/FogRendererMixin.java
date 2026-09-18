package net.mehvahdjukaar.polytone.mixins.fabric;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.content.fluid.FluidPropertyModifier;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(method = "computeFogColor", at = @At("RETURN"))
    private void polytone$fluidFogColormap(Camera camera, float partialTicks, ClientLevel level, int renderDistance,
                                           float darkenWorldAmount, Vector4f dest, CallbackInfo ci) {
        FluidPropertyModifier mod = polytone$submergedFluidModifier(camera, level);
        if (mod == null) return;
        IColorGetter fog = mod.getFogColormap();
        if (fog == null) return;
        BlockPos pos = camera.blockPosition();
        float[] rgb = ColorUtils.unpack(fog.colorInWorld(level.getBlockState(pos), level, pos));
        dest.set(rgb[0], rgb[1], rgb[2], dest.w);
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void polytone$modifyFluidFogShape(Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker,
                                        float darkenWorldAmount, ClientLevel level, CallbackInfoReturnable<FogData> cir) {
        FluidPropertyModifier mod = polytone$submergedFluidModifier(camera, level);
        if (mod == null || !mod.hasFogShape()) {
            return;
        }
        mod.modifyFogShape(cir.getReturnValue(), camera, level);
    }

    @Nullable
    private static FluidPropertyModifier polytone$submergedFluidModifier(Camera camera, ClientLevel level) {
        if (!Polytone.FLUID_MODIFIERS.hasAnyModifier()) return null;
        FluidState fluid = level.getFluidState(camera.blockPosition());
        if (fluid.isEmpty()) return null;
        FluidPropertyModifier mod = Polytone.FLUID_MODIFIERS.getModifierOrVariant(fluid.getType());
        if (mod == null || !FluidPropertyModifier.isCameraSubmerged(camera, level, fluid)) return null;
        return mod;
    }
}
