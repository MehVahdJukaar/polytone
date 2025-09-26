package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin {


    @Inject(method = "stringVertex",
            at = @At(value = "HEAD"))
    private static void polytone$changeLineOffset(float x, float y, float z, VertexConsumer consumer, PoseStack.Pose pose, float f, float g, CallbackInfo ci,
                                                  @Local(ordinal = 0, argsOnly = true) LocalFloatRef xR, @Local(ordinal = 1, argsOnly = true) LocalFloatRef yR, @Local(ordinal = 2, argsOnly = true) LocalFloatRef zR) {
        Vector3f offset = Polytone.COLORS.getFishingLineOffset();
        xR.set((xR.get() + offset.x()));
        yR.set((yR.get() + offset.y()));
        zR.set((zR.get() + offset.z()));
    }

    @ModifyArg(method = "stringVertex",
            index = 0,
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private static int polytone$changeLineColorR(int originalR) {
        if (originalR != 0) return originalR;
        return Polytone.COLORS.getFishingLineColor().r();
    }

    @ModifyArg(method = "stringVertex",
            index = 1,
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private static int polytone$changeLineColorG(int originalR) {
        if (originalR != 0) return originalR;
        return Polytone.COLORS.getFishingLineColor().g();
    }

    @ModifyArg(method = "stringVertex",
            index = 2,
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private static int polytone$changeLineColorB(int originalR) {
        if (originalR != 0) return originalR;
        return Polytone.COLORS.getFishingLineColor().b();
    }

    @ModifyArg(method = "stringVertex",
            index = 3,
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private static int polytone$changeLineColorA(int originalR) {
        if (originalR != 255) return originalR;
        return Polytone.COLORS.getFishingLineColor().a();
    }

}
