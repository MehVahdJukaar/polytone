package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @Inject(method = "blitSprite(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V", at = @At("HEAD"), cancellable = true)
    public void polytone$modifyBlit(TextureAtlasSprite textureAtlasSprite, int i, int j, int k, int l, int m, CallbackInfo ci) {
        // Insert your code here
        if (Polytone.OVERLAY_MODIFIERS.maybeModifyBlit((GuiGraphics) (Object) this, textureAtlasSprite, i, j, k, l, m)){
            ci.cancel();
        }
    }
}
