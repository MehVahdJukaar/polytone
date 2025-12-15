package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
