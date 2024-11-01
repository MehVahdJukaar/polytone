package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin2 {

    @Inject(method = "setupFog", at = @At(value = "TAIL"), cancellable = true)
    private static void polytone$modifyFogShape(Camera camera, FogRenderer.FogMode fogMode, Vector4f vector4f, float f, boolean bl, float g, CallbackInfoReturnable<FogParameters> cir, @Local FogType fogType) {
        if (fogMode == FogRenderer.FogMode.FOG_TERRAIN && fogType == FogType.NONE) {
            var newFog = Polytone.BIOME_MODIFIERS.modifyFogParameters(
                    cir.getReturnValue().start(), cir.getReturnValue().end());
            if (newFog != null) {
                FogParameters old = cir.getReturnValue();
                cir.setReturnValue(new FogParameters(newFog.x, newFog.y, old.shape(), old.red(), old.green(), old.blue(), old.alpha()));
            }

        }
    }

}
