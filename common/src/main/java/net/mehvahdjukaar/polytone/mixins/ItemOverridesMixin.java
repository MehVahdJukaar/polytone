package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemOverrides.class)
public class ItemOverridesMixin {

    @Inject(method = "resolve", at = @At("HEAD"), cancellable = true)
    private void resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed, CallbackInfoReturnable<BakedModel> cir) {
        var newModel = Polytone.ITEM_MODELS.getOverride(stack, level, entity, seed);
        if (newModel != null) {
            cir.setReturnValue(model);
            cir.cancel();
        }
    }

}
