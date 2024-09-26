package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
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

    @Inject(method = "getModel", at = @At(value = "INVOKE",
            shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/client/resources/model/BakedModel;getOverrides()Lnet/minecraft/client/renderer/block/model/ItemOverrides;"))
    private void resolve(ItemStack stack, Level level, LivingEntity entity, int seed, CallbackInfoReturnable<BakedModel> cir,
                         @Local LocalRef<BakedModel> bakedModelLocalRef) {
        var newModel = Polytone.ITEM_MODELS.getOverride(stack,  level, entity, seed);
        if (newModel != null) {
            bakedModelLocalRef.set(newModel);
        }
    }

}
