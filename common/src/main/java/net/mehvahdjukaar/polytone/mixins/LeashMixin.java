package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LeashFeatureRenderer.class, priority = 1300)
public class LeashMixin {

    @Inject(method = "addVertexPair", at = @At("HEAD"), cancellable = true)
    private static void polytone$modifyLeashRender(VertexConsumer builder, Matrix4fc pose, float dx, float dy, float dz, float fudge, float dxOff, float dzOff, int k, boolean backwards, EntityRenderState.LeashState state, CallbackInfo ci) {
        if (PolytoneRenderTypes.addLeashVertexPair(builder, pose, dx, dy, dz, fudge, dxOff, dzOff, k, backwards, state)) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;leash()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private static RenderType polytone$modifyLeashTexture(Operation<RenderType> original) {
        RenderType custom = PolytoneRenderTypes.getLeashRenderType();
        return custom != null ? custom : original.call();
    }
}
