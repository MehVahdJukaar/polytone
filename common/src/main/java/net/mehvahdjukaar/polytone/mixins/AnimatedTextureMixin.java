package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.content.texture.IDayTimeContext;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpriteContents.AnimatedTexture.class)
public abstract class AnimatedTextureMixin implements IDayTimeContext {

    @Unique
    private ITextureDeltaProvider polytone$mode = PolyDeltaProvider.VANILLA;
    @Unique
    private int polytone$dayDuration = 0;

    @Override
    public ITextureDeltaProvider polytone$getDeltaProvider() {
        return polytone$mode;
    }

    @Override
    public void polytone$setDeltaProvider(ITextureDeltaProvider mode) {
        this.polytone$mode = mode;
        if (mode == PolyDeltaProvider.DAY_TIME) {
            polytone$dayDuration = SharedConstants.TICKS_PER_GAME_DAY;
        } else if (mode == PolyDeltaProvider.GAME_TIME) {
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
        if (polytone$mode != PolyDeltaProvider.VANILLA) {

            ((IDayTimeContext) original).polytone$setDeltaProvider(polytone$mode);
            ((IDayTimeContext) original).polytone$setTimeCycleDuration(polytone$dayDuration);
        }
        return original;
    }
}
