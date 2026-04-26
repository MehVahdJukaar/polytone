package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.content.particle.custom.render.ModelParticleRenderGroup;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(QuadParticleGroup.class)
public class QuadParticleGroupMixin {

    @Redirect(method = "extractRenderState",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;pointInFrustum(DDD)Z"))
    public boolean polytone$accurateFrustumIntersect(Frustum instance, double d, double e, double f,
                                                     @Local SingleQuadParticle particle, @Local(argsOnly = true) Camera camera) {
        return ModelParticleRenderGroup.particleInFrustum(instance, particle, camera);
    }
}
