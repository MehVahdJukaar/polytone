package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    @Shadow
    protected ClientLevel level;

    @ModifyReturnValue(method = "makeParticle", at = @At("RETURN"))
    public @Nullable <T extends ParticleOptions> Particle polytone$applyModifiers(@Nullable Particle original,
                                                                                  @Local(argsOnly = true) T particleData) {
        if (original != null) Polytone.PARTICLE_MODIFIERS.maybeModify(particleData, this.level, original);
        return original;
    }

    @Inject(method = "reload", at = @At(value = "HEAD"))
    public void polytone$addPackSpriteSets(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, Executor executor, Executor executor2, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        Polytone.CUSTOM_PARTICLES.addSpriteSets(resourceManager);
    }
}
