package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


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
