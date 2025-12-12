package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
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

    // We now inject at the top of the method, and cancel if we overwrote it.
    @Inject(method = "renderLeash", at = @At("HEAD"))
    private static void polytone$modifyLeashTexture(Matrix4f matrix4f, MultiBufferSource instance, EntityRenderState.LeashState leashState, CallbackInfo ci) {
      /* FIXME(dannyb) - need to figure out what we want to do here */
        /*
        var consumer = PolytoneRenderTypes.getLeashVertexConsumer(instance);
        if (consumer != null) ci.cancel();*/

    }
}
