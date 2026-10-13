package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/client/sounds/SoundManager$Preparations;",
            at = @At("HEAD"))
    private void polytone$loadConfigsForSoundConditions(ResourceManager resourceManager, ProfilerFiller profiler,
                                                        CallbackInfoReturnable<?> cir) {
        Polytone.CONFIGS.loadCurrentReloadConfigs(resourceManager);
    }

    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/client/sounds/SoundManager$Preparations;",
            at = @At("RETURN"))
    private void polytone$clearConfigsForSoundConditions(ResourceManager resourceManager, ProfilerFiller profiler,
                                                         CallbackInfoReturnable<?> cir) {
        Polytone.CONFIGS.clearCurrentPackConfigs();
    }
}
