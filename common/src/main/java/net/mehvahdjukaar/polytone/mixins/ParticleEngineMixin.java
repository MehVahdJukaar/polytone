package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.light.ColoredLightsTracker;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.content.block.TickSource;
import net.mehvahdjukaar.polytone.content.particle.custom.PolytoneAsyncParticleHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
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
        if (original != null) {
            Polytone.PARTICLE_MODIFIERS.maybeModify(particleData, this.level, original);
            ColoredLightsTracker.onParticleCreated(particleData.getType(), original);
        }
        return original;
    }

    @Inject(method = "reload", at = @At(value = "HEAD"))
    public void polytone$addPackSpriteSets(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        Polytone.CUSTOM_PARTICLES.addSpriteSets(resourceManager);
    }

    @ModifyArg(method = "method_18125",
            require = 0, //low priority
            at = @At(value = "INVOKE", target = "Lcom/google/common/collect/EvictingQueue;create(I)Lcom/google/common/collect/EvictingQueue;"))
    private static int polytone$modifyEvictingQueueSize(int size) {
        return size * 50;
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    public void polytone$addExtraDestroyParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!state.isAir()) {
            Polytone.BLOCK_MODIFIERS.runTickers(state, this.level, pos, TickSource.BLOCK_BROKEN);
        }
    }

    //fabric only since neo its deprecated
    @Inject(method = "render", at = @At("HEAD"))
    public void onRenderLast(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        PolytoneRenderTypes.cacheMatrices();
    }

    // Async custom particle batch: joined before anything reads (render, via LevelRendererMixin) or
    // mutates (next tick, level change) particle state; dispatched at tick TAIL so the batch overlaps
    // the rest of the game tick.
    @Inject(method = "tick", at = @At("HEAD"))
    private void polytone$joinBeforeTick(CallbackInfo ci) {
        PolytoneAsyncParticleHandler.awaitTicks();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void polytone$dispatchAsyncParticleTicks(CallbackInfo ci) {
        if (Polytone.CONFIGS.particlesOffThread.get()) {
            PolytoneAsyncParticleHandler.dispatch();
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void polytone$joinBeforeLevelChange(CallbackInfo ci) {
        PolytoneAsyncParticleHandler.awaitTicks();
    }

    @Inject(method = "clearParticles", at = @At("HEAD"))
    private void polytone$joinBeforeClear(CallbackInfo ci) {
        PolytoneAsyncParticleHandler.awaitTicks();
    }
}
