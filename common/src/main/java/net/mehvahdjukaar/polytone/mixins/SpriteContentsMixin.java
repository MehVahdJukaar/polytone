package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.content.texture.IDayTimeContext;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin {


    @ModifyReturnValue(method = "createAnimatedTexture", at = @At("RETURN"))
    public SpriteContents.AnimatedTexture polytone$addWorldTimeTextureData(SpriteContents.AnimatedTexture original,
                                                                           @Local(argsOnly = true) AnimationMetadataSection metadata) {
        if (original != null) {
            ((IDayTimeContext) original).polytone$setDeltaProvider(
                    ((IDayTimeContext) (Object) metadata).polytone$getDeltaProvider());
            ((IDayTimeContext) original).polytone$setTimeCycleDuration(
                    ((IDayTimeContext) (Object) metadata).polytone$getTimeCycleDuration());
        }
        return original;
    }
}
