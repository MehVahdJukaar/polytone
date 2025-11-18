package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.block.TickSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
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


@Mixin(ParticleResources.class)
public abstract class ParticleResourcesMixin {


    @Inject(method = "reload", at = @At(value = "HEAD"))
    public void polytone$addPackSpriteSets(PreparableReloadListener.SharedState sharedState, Executor executor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor executor2, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        Polytone.CUSTOM_PARTICLES.addSpriteSets(sharedState.resourceManager());
    }

}
