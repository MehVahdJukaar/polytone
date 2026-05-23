package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

// hideous. We need to run CIM before model baking so we can bake extra auto generated models
@Mixin(ModelManager.class)
public class ModelManagerMixin {

    @Inject(method = "loadBlockModels", at = @At("HEAD"))
    private static void polytone$loadCustomItemModels(ResourceManager resourceManager, Executor executor, CallbackInfoReturnable<CompletableFuture> cir) {
        try {
            Polytone.onEarlyPackLoad(resourceManager);
        } catch (Exception e) {
            Polytone.LOGGER.error("Polytone: failed to process early reload", e);
            Polytone.displayEarlyReloadFailedToast();
        }
    }
}
