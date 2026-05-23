package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.PostChainEffect;
import net.mehvahdjukaar.polytone.content.shaders.PostShadersManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PostPass.class)
public class PostPassMixin {

    @Inject(method = "lambda$addToFrame$0", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"))
    private static void poly$onPostPass(FramePass pass, Identifier id, ResourceHandle handle, CallbackInfoReturnable<ResourceHandle> cir, @Local RenderPass pass) {
        Polytone.POST_SHADERS.setupExtraUniforms(pass);
        PostChainEffect effect = Polytone.POST_SHADERS.getEffectForPass((PostPass) (Object) this);
        if (effect != null) {
            effect.bindExpressionUniforms(pass);
        }
    }
}
