package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// would be better in itemoverrides class but idk why it doesnt work there
@Mixin(ItemRenderer.class)
public class ItemOverridesMixin {
/*
    @Inject(method = "renderItem",
            at = @At(value = "HEAD"))
    private static void resolve(ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                                MultiBufferSource multiBufferSource, int i, int j, int[] is,
                                BakedModel bakedModel, RenderType renderType, ItemStackRenderState.FoilType foilType,
                                CallbackInfo ci, @Local LocalRef<BakedModel> bakedModelLocalRef) {
        var newModel = Polytone.ITEM_MODELS.getOverride(stack,  level, entity, seed);
        if (newModel != null) {
            bakedModelLocalRef.set(newModel);
        }
    }
*/
}
