package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbRenderer.class)
public class XpOrbMixin {

    @Unique
    private static float @Nullable [] polytone$specialColor = null;

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ExperienceOrbRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"))
    private void polytone$startRenderOrb(ExperienceOrbRenderState experienceOrbRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        polytone$specialColor = Polytone.COLORS.getXpOrbColor(experienceOrbRenderState, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ExperienceOrbRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("TAIL"))
    private void polytone$endRenderOrb(ExperienceOrbRenderState experienceOrbRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        polytone$specialColor = null;
    }

    @Inject(method = "vertex", at = @At("HEAD"), cancellable = true)
    private static void polytone$changeColor(VertexConsumer consumer, PoseStack.Pose matrix, float x, float y, int red, int green, int blue, float texU, float texV, int packedLight, CallbackInfo ci) {
        if (polytone$specialColor != null) {
            ci.cancel();
            consumer.addVertex(matrix, x, y, 0.0F).setColor(polytone$specialColor[0], polytone$specialColor[1],
                            polytone$specialColor[2], 0.5f)
                    .setUv(texU, texV).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(packedLight).setNormal(matrix, 0.0F, 1.0F, 0.0F);

        }
    }
}
