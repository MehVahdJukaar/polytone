package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.content.shaders.light.IFaceInLightBuilder;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneShaderPatcher;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin {

    @ModifyArg(method = "putBlockBakedQuad(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V"), index = 7)
    private int polytone$addFaceToLight(int lightCoords, @Local(argsOnly = true) BakedQuad quad) {
        if (!(this instanceof IFaceInLightBuilder b) || !b.polytone$storesFaceInLight()) return lightCoords;
        return lightCoords | PolytoneShaderPatcher.faceBits(quad.direction()) << PolytoneShaderPatcher.LIGHT_DIRECTION_BITS_SHIFT;
    }
}
