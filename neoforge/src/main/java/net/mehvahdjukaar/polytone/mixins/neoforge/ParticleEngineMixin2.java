package net.mehvahdjukaar.polytone.mixins.neoforge;

import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;


@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin2 {

    @Inject(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At("HEAD"))
    public void onRenderLast(LightTexture arg, Camera arg2, float f, Frustum frustum, Predicate<ParticleRenderType> renderTypePredicate, CallbackInfo ci) {
        PolytoneRenderTypes.cacheMatrices(); //forge v
    }
}
