package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// would be better in itemoverrides class but idk why it doesnt work there
@Mixin(ItemRenderer.class)
public class ItemOverridesMixin {

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void resolve(ItemStack stack, Level level, LivingEntity entity, int seed, CallbackInfoReturnable<BakedModel> cir) {
        var newModel = Polytone.ITEM_MODELS.getOverride(stack,  level, entity, seed);
        if (newModel != null) {
            cir.setReturnValue(newModel);
            cir.cancel();
        }
    }

}
