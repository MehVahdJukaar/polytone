package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.caffeinemc.mods.sodium.client.render.immediate.CloudRenderer;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(CloudRenderer.class)
public class SodiumCloudRendererMixin {
//
//
//    @Inject(method = "render",
//            require = 0,
//            at = @At(value = "INVOKE",
//                    target = "Lnet/minecraft/client/Camera;getPosition()Lnet/minecraft/world/phys/Vec3;"))
//    private void polytone$whyDoesSodiumHaveToReplaceEntireClasses(Camera camera, ClientLevel level, Matrix4f projectionMatrix, Matrix4f modelView, float ticks, float tickDelta, int color, CallbackInfo ci, @Local(ordinal = 2) LocalFloatRef height) {
//        var f = Polytone.DIMENSION_MODIFIERS.modifyCloudHeight(level);
//        f.ifPresent(height::set);
//    }
}
