package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.texture.DayTimeTexture;
import net.minecraft.SharedConstants;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AnimationMetadataSection.class)
public class AnimationMetadataSectionMixin implements DayTimeTexture {
    @Unique
    private Mode polytone$mode = Mode.VANILLA;
    @Unique
    private int polytone$dayDuration = SharedConstants.TICKS_PER_GAME_DAY;

    @Override
    public Mode polytone$getMode() {
        return this.polytone$mode;
    }

    @Override
    public void polytone$setMode(Mode mode) {
        this.polytone$mode = mode;
    }

    @Override
    public int polytone$getTimeCycleDuration() {
        return polytone$dayDuration;
    }

    @Override
    public void polytone$setTimeCycleDuration(int duration) {
        this.polytone$dayDuration = duration;
    }
}
