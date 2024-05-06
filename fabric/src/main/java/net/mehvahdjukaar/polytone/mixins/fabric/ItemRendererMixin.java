package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.utils.fabric.SeparateTransformsModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @WrapOperation(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/ItemTransform;apply(ZLcom/mojang/blaze3d/vertex/PoseStack;)V"))
    public void render(ItemTransform instance, boolean leftHand, PoseStack poseStack, Operation<Void> original,
                       @Local LocalRef<BakedModel> model, @Local ItemDisplayContext displayContext) {
        if (model.get() instanceof SeparateTransformsModel.Baked sp) {
            var m = sp.applyTransform(displayContext, poseStack, leftHand);
            model.set(m);
        } else {
            original.call(instance, leftHand, poseStack);
        }
    }
}
