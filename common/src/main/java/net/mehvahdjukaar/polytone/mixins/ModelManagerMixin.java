package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

// super ugly. We need to run CIM before model baking so we can bake extra auto generated models
@Mixin(ModelManager.class)
public class ModelManagerMixin {

    @Inject(method = "method_45895", at = @At("HEAD"))
    private static void polytone$loadCustomItemModels(ResourceManager resourceManager, CallbackInfoReturnable<Map> cir) {
        Polytone.ITEM_MODIFIERS.earlyProcess(resourceManager);
        Polytone.ITEM_MODELS.earlyProcess(resourceManager);
        Polytone.LOGGER.info("Polytone: computed custom item models from thread {}", Thread.currentThread());
    }
}
