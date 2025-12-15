package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleOptions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


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

//    @Inject(method = "reload", at = @At(value = "HEAD"))
//    public void polytone$addPackSpriteSets(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, Executor executor, Executor executor2, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
//        Polytone.CUSTOM_PARTICLES.addSpriteSets(resourceManager);
//    }
//
//    @ModifyArg(method = "method_18125",
//            require = 0, //low priority
//            at = @At(value = "INVOKE", target = "Lcom/google/common/collect/EvictingQueue;create(I)Lcom/google/common/collect/EvictingQueue;"))
//    private static int polytone$modifyEvictingQueueSize(int size) {
//        return size * 50;
//    }
//
//    @Inject(method = "destroy", at = @At("HEAD"))
//    public void polytone$addExtraDestroyParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
//        if (!state.isAir()) {
//            Polytone.BLOCK_MODIFIERS.runTickers(state, this.level, pos, TickSource.BLOCK_BROKEN);
//        }
//    }

    //fabric only since neo its deprecated

    @Inject(method = "render", at = @At("HEAD"))
    public void onRenderLast(Camera camera, float f, MultiBufferSource.BufferSource bufferSource, CallbackInfo ci) {
        PolytoneRenderTypes.cacheMatrices();
    }

    /*
    @WrapOperation(method = "crack", at = @At(value = "TAIL"))
    public void polytone$modifyCrackParticles(ParticleEngine instance, Particle particle, Operation<Void> original,
                                              @Local BlockState state, @Local(argsOnly = true) BlockPos pos) {
        if(!state.isAir()){
            Polytone.BLOCK_MODIFIERS.maybeSpawnBreakParticles(state, this.level, pos, Direction.UP);
        }
    }*/
}
