package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.slotify.GuiDepthTarget;
import net.mehvahdjukaar.polytone.content.slotify.GuiDepthTargetAware;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin implements GuiDepthTargetAware {

    @Shadow
    @Final
    private GuiRenderState guiRenderState;

    @Shadow
    protected abstract void innerBlit(RenderPipeline renderPipeline, Identifier location, int x0, int x1, int y0, int y1, float u0, float u1, float v0, float v1, int color);

    @Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V",
            at = @At(value = "INVOKE",
                    shift = At.Shift.BEFORE,
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V"), cancellable = true)
    public void polytone$modifyBlit(RenderPipeline pipeline, TextureAtlasSprite sprite,
                                    int x, int y, int width, int height, int color, CallbackInfo ci) {
        if (Polytone.OVERLAY_MODIFIERS.maybeModifyBlit((GuiGraphicsExtractor) (Object) this, pipeline,
                sprite, x, y, width, height, color)) {
            ci.cancel();
        }
    }

    //cut blit
    @Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIIIIIII)V",
            at = @At(value = "INVOKE",
                    shift = At.Shift.BEFORE,
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V"), cancellable = true)
    public void polytone$modifyBlit(RenderPipeline pipeline, TextureAtlasSprite sprite, int textureWidth, int textureHeight, int uPosition, int vPosition,
                                    int x, int y, int uWidth, int vHeight, int color, CallbackInfo ci) {
        if (Polytone.OVERLAY_MODIFIERS.maybeModifyBlit((GuiGraphicsExtractor) (Object) this, pipeline,
                sprite, x, y, textureWidth, textureHeight,
                uPosition, vPosition, uWidth, vHeight, color)) {
            ci.cancel();
        }
    }

    @Override
    public void polytone$renderInNode(GuiDepthTarget nodeTarget, Runnable renderFunction) {
        ((GuiDepthTargetAware) this.guiRenderState).polytone$renderInNode(nodeTarget, renderFunction);
    }

    @Unique
    @Override
    public void polytone$innerBlit(RenderPipeline pipeline, Identifier location,
                                   int x0, int x1, int y0, int y1,
                                   float u0, float u1, float v0, float v1, int color) {
        this.innerBlit(pipeline, location, x0, x1, y0, y1, u0, u1, v0, v1, color);
    }
}
