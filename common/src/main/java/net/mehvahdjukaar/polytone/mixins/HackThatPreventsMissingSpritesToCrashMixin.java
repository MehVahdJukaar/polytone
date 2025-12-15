package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.misc.DummySprite;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

//don't ask why. only happens on log off after disabling the pack
@Mixin(ParticleResources.MutableSpriteSet.class)
public class HackThatPreventsMissingSpritesToCrashMixin {

    @Shadow
    private List<TextureAtlasSprite> sprites;

    @Inject(method = "get(II)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;", at = @At("HEAD"), cancellable = true)
    public void polytone$setNullSprite(int i, int j, CallbackInfoReturnable<TextureAtlasSprite> cir) {
        if (this.sprites == null) {
            cir.setReturnValue(DummySprite.INSTANCE);
        }
    }

    @Inject(method = "get(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;",
            at = @At("HEAD"), cancellable = true)
    public void polytone$setNullSprite(RandomSource randomSource, CallbackInfoReturnable<TextureAtlasSprite> cir) {
        if (this.sprites == null) {
            cir.setReturnValue(DummySprite.INSTANCE);
        }
    }
}
