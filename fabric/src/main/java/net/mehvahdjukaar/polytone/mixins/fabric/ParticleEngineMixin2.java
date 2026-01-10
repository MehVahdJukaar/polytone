package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin2 {

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE",
                    ordinal = 0,
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;applyModelViewMatrix()V"))
    public void polytone$cacheMatrices(Operation<Void> original) {
       original.call();
        PolytoneRenderTypes.cacheMatrices();
    }
}
