package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.color.PaintingRenderType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PaintingRenderer.class)
public abstract class PaintingRendererMixin {

    @WrapOperation(method = "render(Lnet/minecraft/world/entity/decoration/Painting;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType polytone$swapPaintingRenderType(ResourceLocation atlasLocation, Operation<RenderType> original) {
        PaintingRenderType type = Polytone.COLORS.getPaintingRenderType();
        if (type != null) {
            return type.create(atlasLocation);
        }
        return original.call(atlasLocation);
    }
}
