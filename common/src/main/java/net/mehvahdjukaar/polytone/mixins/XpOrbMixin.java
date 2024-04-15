package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.ExperienceOrb;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbRenderer.class)
public class XpOrbMixin {

    @Nullable
    @Unique
    private static float[] polytone$specialColor = null;

    @Inject(method = "render(Lnet/minecraft/world/entity/ExperienceOrb;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void polytone$startRenderOrb(ExperienceOrb entity, float entityYaw, float partialTicks, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight,CallbackInfo ci) {
        polytone$specialColor = Polytone.COLORS.getXpOrbColor(entity, partialTicks);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/ExperienceOrb;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL"))
    private void polytone$endRenderOrb(ExperienceOrb entity, float entityYaw, float partialTicks, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight,CallbackInfo ci) {
        polytone$specialColor = null;
    }

    @Inject(method = "vertex", at = @At("HEAD"), cancellable = true)
    private static void polytone$changeColor(VertexConsumer consumer, Matrix4f matrix, Matrix3f matrixNormal, float x, float y, int red, int green, int blue, float texU, float texV, int packedLight, CallbackInfo ci) {
        if(polytone$specialColor != null){
            ci.cancel();
            consumer.vertex(matrix, x, y, 0.0F).color(polytone$specialColor[0], polytone$specialColor[1],
                            polytone$specialColor[2], 0.5f)
                    .uv(texU, texV).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(packedLight).normal(matrixNormal, 0.0F, 1.0F, 0.0F).endVertex();

        }
    }
}
