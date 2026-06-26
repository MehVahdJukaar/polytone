package net.mehvahdjukaar.polytone.mixins;

import net.minecraft.core.particles.ParticleLimit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//Needed because vanilla has a hashmap of these and they should actually go by identity otherwise all particles with same limit will be grouped together...
@Mixin(ParticleLimit.class)
public abstract class ParticleLimitMixin {

    @Inject(method = "equals", at = @At("HEAD"), cancellable = true)
    private void polytone$identityEquals(Object other, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this == other);
    }

    @Inject(method = "hashCode", at = @At("HEAD"), cancellable = true)
    private void polytone$identityHashCode(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(System.identityHashCode(this));
    }
}
