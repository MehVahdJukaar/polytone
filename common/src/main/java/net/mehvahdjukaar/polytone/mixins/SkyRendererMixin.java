package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SkyRenderer.class)
public class SkyRendererMixin {

    @ModifyArg(method = {"renderSun(FLcom/mojang/blaze3d/vertex/PoseStack;)V",
            "renderMoon(Lnet/minecraft/world/level/MoonPhase;FLcom/mojang/blaze3d/vertex/PoseStack;)V"},
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"))
    private RenderPipeline polytone$translucentSunAndMoon(RenderPipeline pipeline) {
        return Polytone.DIMENSION_MODIFIERS.translucentSunAndMoon() ? PolytoneRenderTypes.TRANSLUCENT_CELESTIAL_PIPELINE : pipeline;
    }

    //fake sky render distance can be past the real far plane, which clips the sky disc (512 out). sky fog reads the unscaled positions so here we shrink it to look identical
    @ModifyArg(method = "renderSkyDisc", index = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private Matrix4fc polytone$hackfitSkyInFarPlane(Matrix4fc modelView) {
        if (Polytone.COLORS.getSkyRenderDistance() == null) {
            return modelView;
        }
        float scale = Minecraft.getInstance().levelRenderer.levelRenderState.cameraRenderState.depthFar / 520f;
        return scale < 1 ? new Matrix4f(modelView).scale(scale) : modelView;
    }

    @ModifyExpressionValue(method = "buildStars", at = @At(value = "CONSTANT", args = "intValue=1500"))
    private int polytone$modifyStarCount(int original) {
        return Polytone.COLORS.getStarCount(original);
    }

    @ModifyVariable(method = "buildStars", at = @At("STORE"), ordinal = 4)
    private float polytone$modifyStarSize(float original) {
        return Polytone.COLORS.getStarSize(original);
    }
}
