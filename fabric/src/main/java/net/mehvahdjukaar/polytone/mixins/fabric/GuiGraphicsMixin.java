package net.mehvahdjukaar.polytone.mixins.fabric;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @Shadow
    @Final
    private MultiBufferSource.BufferSource bufferSource;

    @Inject(method = "blitSprite(Ljava/util/function/Function;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V", at = @At("HEAD"), cancellable = true)
    public void polytone$modifyBlit(Function<ResourceLocation, RenderType> function, TextureAtlasSprite textureAtlasSprite,
                                    int x, int y, int width, int height, int offset, CallbackInfo ci) {
        if (Polytone.OVERLAY_MODIFIERS.maybeModifyBlit((GuiGraphics) (Object) this,
                function, this.bufferSource, textureAtlasSprite,
                x, y, width, height, offset)) {
            ci.cancel();
        }
    }
}
