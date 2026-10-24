package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightningBoltRenderer.class)
public abstract class LightningBoltRendererMixin {

    @WrapOperation(method = "quad",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private static VertexConsumer polytone$changeBoltColor(VertexConsumer consumer, float r, float g, float b, float a,
                                                           Operation<VertexConsumer> original) {
        Integer c = Polytone.COLORS.getLightningBoltColor();
        if (c == null) return original.call(consumer, r, g, b, a);

        float[] col = ColorUtils.unpackWithAlpha(c, a);
        return original.call(consumer, col[0], col[1], col[2], col[3]);
    }
}
