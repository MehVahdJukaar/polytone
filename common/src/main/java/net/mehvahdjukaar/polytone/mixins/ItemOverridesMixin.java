package net.mehvahdjukaar.polytone.mixins;

import net.minecraft.client.renderer.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;

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
