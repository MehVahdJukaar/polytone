package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.content.texture.IDeltaProviderContext;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin {


    @ModifyReturnValue(method = "createAnimatedTexture", at = @At("RETURN"))
    public SpriteContents.AnimatedTexture polytone$addWorldTimeTextureData(SpriteContents.AnimatedTexture original,
                                                                           @Local(argsOnly = true) AnimationMetadataSection metadata) {
        if(original != null) {
            ((IDeltaProviderContext) original).polytone$setMode(
                    ((IDeltaProviderContext) metadata).polytone$getMode());
            ((IDeltaProviderContext) original).polytone$setTimeCycleDuration(
                    ((IDeltaProviderContext) metadata).polytone$getTimeCycleDuration());
        }
        return original;
    }
}
