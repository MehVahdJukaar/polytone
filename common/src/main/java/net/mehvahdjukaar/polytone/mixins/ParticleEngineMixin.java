package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    @ModifyReturnValue(method = "makeParticle", at = @At("RETURN"))
    public @Nullable <T extends ParticleOptions> Particle polytone$applyModifiers(@Nullable Particle original, @Local T particleData){
        Polytone.PARTICLE_MODIFIERS.maybeModify(particleData.getType(), original);
        return original;
    }
}
