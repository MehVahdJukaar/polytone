package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.particles.ParticleManager;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "makeParticle", at = @At("RETURN"))
    public<T extends ParticleOptions> void polytone$applyModifiers(T particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfoReturnable<Particle> cir){
        ParticleManager.modify(cir.getReturnValue());
    }
}
