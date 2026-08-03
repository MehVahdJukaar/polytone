package net.mehvahdjukaar.polytone.mixins;

import com.google.common.collect.EvictingQueue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.content.block.TickSource;
import net.mehvahdjukaar.polytone.content.particle.custom.PolytoneAsyncParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ParticleUtils;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

    // Batch dispatched at tick() TAIL overlaps the rest of the game tick and early frame render,
    // joined before anything reads (extract) or mutates (next tick, level change) particle state.
    @Inject(method = "tick", at = @At("HEAD"))
    private void polytone$joinBeforeTick(CallbackInfo ci) {
        PolytoneAsyncParticles.awaitTicks();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void polytone$dispatchAsyncParticleTicks(CallbackInfo ci) {
        if (Polytone.CONFIGS.particlesOffThread.get()) {
            PolytoneAsyncParticles.dispatch();
        }
    }

    @Inject(method = "extract", at = @At("HEAD"))
    private void polytone$joinBeforeExtract(CallbackInfo ci) {
        PolytoneAsyncParticles.awaitTicks();
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void polytone$joinBeforeLevelChange(CallbackInfo ci) {
        PolytoneAsyncParticles.awaitTicks();
    }

    @Inject(method = "clearParticles", at = @At("HEAD"))
    private void polytone$joinBeforeClear(CallbackInfo ci) {
        PolytoneAsyncParticles.awaitTicks();
    }

}
