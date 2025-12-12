package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.GuiDepthTarget;
import net.mehvahdjukaar.polytone.utils.GuiDepthTargetAware;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin implements GuiDepthTargetAware {

    @Shadow
    @Final
    private GuiRenderState guiRenderState;

    @Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V",
            at = @At(value = "INVOKE",
                    shift = At.Shift.BEFORE,
                    target = "Lnet/minecraft/client/gui/GuiGraphics;innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V"), cancellable = true)
    public void polytone$modifyBlit(RenderPipeline pipeline, TextureAtlasSprite sprite,
                                    int x, int y, int width, int height, int color, CallbackInfo ci) {
        if (Polytone.OVERLAY_MODIFIERS.maybeModifyBlit((GuiGraphics) (Object) this, pipeline,
                sprite, x, y, width, height, color)) {
            ci.cancel();
        }
    }

    //cut blit
    @Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIIIIIII)V",
            at = @At(value = "INVOKE",
                    shift = At.Shift.BEFORE,
                    target = "Lnet/minecraft/client/gui/GuiGraphics;innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V"), cancellable = true)
    public void polytone$modifyBlit(RenderPipeline pipeline, TextureAtlasSprite sprite, int textureWidth, int textureHeight, int uPosition, int vPosition,
                                    int x, int y, int uWidth, int vHeight, int color, CallbackInfo ci) {
        if (Polytone.OVERLAY_MODIFIERS.maybeModifyBlit((GuiGraphics) (Object) this, pipeline,
                sprite, x, y, textureWidth, textureHeight,
                uPosition, vPosition, uWidth, vHeight, color)) {
            ci.cancel();
        }
    }

    @Override
    public void renderInNode(GuiDepthTarget nodeTarget, Runnable renderFunction) {
        ((GuiDepthTargetAware) this.guiRenderState).renderInNode(nodeTarget, renderFunction);
    }
}
