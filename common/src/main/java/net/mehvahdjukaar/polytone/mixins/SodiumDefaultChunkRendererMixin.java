package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(DefaultChunkRenderer.class)
public abstract class SodiumDefaultChunkRendererMixin {

    @WrapOperation(method = "render", require = 0, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"))
    private void polytone$bindVanillaGlobals(RenderPass pass, RenderPipeline pipeline, Operation<Void> original) {
        original.call(pass, pipeline);
        GpuBuffer globals = RenderSystem.getGlobalSettingsUniform();
        if (globals != null) {
            //we need vanilla global here
            pass.setUniform("Globals", globals);
        }
    }
}
