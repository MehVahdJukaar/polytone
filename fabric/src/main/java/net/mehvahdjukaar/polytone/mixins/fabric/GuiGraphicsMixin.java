package net.mehvahdjukaar.polytone.mixins.fabric;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @Inject(method = "blitSprite(Ljava/util/function/Function;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V", at = @At("HEAD"), cancellable = true)
    public void polytone$modifyBlit(Function<ResourceLocation, RenderType> function, TextureAtlasSprite textureAtlasSprite, int x, int y, int offset, int width, int height, CallbackInfo ci) {
        if (Polytone.OVERLAY_MODIFIERS.maybeModifyBlit((GuiGraphics) (Object) this, function, textureAtlasSprite, x, y, offset, width, height)){
            ci.cancel();
        }
    }
}
