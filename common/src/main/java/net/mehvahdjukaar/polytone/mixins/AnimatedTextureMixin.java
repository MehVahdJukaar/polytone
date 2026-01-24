package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.content.texture.IDeltaProvider;
import net.mehvahdjukaar.polytone.content.texture.IDeltaProviderContext;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpriteContents.AnimatedTexture.class)
public abstract class AnimatedTextureMixin implements IDeltaProviderContext {

    @Unique
    private IDeltaProvider polytone$mode = IDeltaProvider.PresetProvider.VANILLA;
    @Unique
    private int polytone$dayDuration = 0;

    @Override
    public IDeltaProvider polytone$getDeltaProvider() {
        return polytone$mode;
    }

    @Override
    public void polytone$setDeltaProvider(IDeltaProvider mode) {
        this.polytone$mode = mode;
        if (mode == IDeltaProvider.PresetProvider.DAY_TIME) {
            polytone$dayDuration = SharedConstants.TICKS_PER_GAME_DAY;
        } else if (mode == IDeltaProvider.PresetProvider.GAME_TIME) {
            polytone$dayDuration = 1;
        }
    }

    @Override
    public void polytone$setTimeCycleDuration(int duration) {
        this.polytone$dayDuration = duration;
    }

    @Override
    public int polytone$getTimeCycleDuration() {
        return polytone$dayDuration;
    }

    @ModifyReturnValue(method = "createAnimationState", at = @At("RETURN"))
    public SpriteContents.AnimationState polytone$modifyTicker(SpriteContents.AnimationState original) {
        if (polytone$mode != IDeltaProvider.PresetProvider.VANILLA) {

            ((IDeltaProviderContext) original).polytone$setDeltaProvider(polytone$mode);
            ((IDeltaProviderContext) original).polytone$setTimeCycleDuration(polytone$dayDuration);
        }
        return original;
    }
}
