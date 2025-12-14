package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LeashFeatureRenderer.class, priority = 1300)
public class LeashMixin {

    @Inject(method = "addVertexPair", at = @At("HEAD"), cancellable = true)
    private static void polytone$modifyLeashRender(VertexConsumer vertexConsumer, Matrix4f matrix4f, float f, float g, float h, float i, float j, float k, int l, boolean bl, EntityRenderState.LeashState leashState, CallbackInfo ci) {
        if (PolytoneRenderTypes.addLeashVertexPair(vertexConsumer, matrix4f, f, g, h, i, j, k, l, bl, leashState)) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "renderLeash", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private static VertexConsumer polytone$modifyLeashTexture(MultiBufferSource instance, RenderType renderType, Operation<VertexConsumer> original) {
        var consumer = PolytoneRenderTypes.getLeashVertexConsumer(instance);
        if (consumer != null) return consumer;
        else return original.call(instance, renderType);
    }
}
