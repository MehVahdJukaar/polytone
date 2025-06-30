package net.mehvahdjukaar.polytone.mixins.fabric;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @Inject(method = "blitSprite(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V",
            at = @At(value = "INVOKE",
                    shift = At.Shift.BEFORE,
                    target = "Lnet/minecraft/client/gui/GuiGraphics;innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V"), cancellable = true)
    public void polytone$modifyBlit(TextureAtlasSprite sprite, int x, int y, int offset, int width, int height, CallbackInfo ci) {
        if (Polytone.OVERLAY_MODIFIERS.maybeModifyBlit((GuiGraphics) (Object) this, sprite, x, y, offset, width, height)){
            ci.cancel();
        }
    }

    //cut blit
    @Inject(method = "blitSprite(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIIIIIII)V",
            at = @At(value = "INVOKE",
                    shift = At.Shift.BEFORE,
                    target = "Lnet/minecraft/client/gui/GuiGraphics;innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V"), cancellable = true)
    public void polytone$modifyBlit(TextureAtlasSprite sprite, int textureWidth, int textureHeight, int uPosition, int vPosition,
                                    int x, int y, int offset, int uWidth, int vHeight, CallbackInfo ci) {
        if (Polytone.OVERLAY_MODIFIERS.maybeModifyBlit((GuiGraphics) (Object) this, sprite, x, y, offset, textureWidth, textureHeight,
                uPosition, vPosition, uWidth, vHeight)) {
            ci.cancel();
        }
    }
}
