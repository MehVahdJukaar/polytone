package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogEnvironment.class)
public abstract class FogRendererMixin2 {
    /* Disable biome fog modification for now */
//    @Inject(method = "setupFog", at = @At(value = "TAIL"), cancellable = true)
    private static void polytone$modifyFogShape(FogData par1, Entity par2, BlockPos par3, ClientLevel par4, float par5, DeltaTracker par6, CallbackInfo ci, @Local FogType fogType) {
//        if (fogMode == FogRenderer.FogMode.FOG_TERRAIN && fogType == FogType.NONE) {
//            var newFog = Polytone.BIOME_MODIFIERS.modifyFogParameters(
//                    cir.getReturnValue().start(), cir.getReturnValue().end());
//            if (newFog != null) {
//                FogParameters old = cir.getReturnValue();
//                cir.setReturnValue(new FogParameters(newFog.x, newFog.y, old.shape(), old.red(), old.green(), old.blue(), old.alpha()));
//            }
//
//        }
    }

}