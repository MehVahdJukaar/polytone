package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.texture.IDeltaProvider;
import net.mehvahdjukaar.polytone.texture.IDeltaProviderContext;
import net.minecraft.SharedConstants;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AnimationMetadataSection.class)
public class AnimationMetadataSectionMixin implements IDeltaProviderContext {
    @Unique
    private IDeltaProvider polytone$mode = IDeltaProvider.PresetProvider.VANILLA;
    @Unique
    private int polytone$dayDuration = SharedConstants.TICKS_PER_GAME_DAY;

    @Override
    public IDeltaProvider polytone$getMode() {
        return this.polytone$mode;
    }

    @Override
    public void polytone$setMode(IDeltaProvider mode) {
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
