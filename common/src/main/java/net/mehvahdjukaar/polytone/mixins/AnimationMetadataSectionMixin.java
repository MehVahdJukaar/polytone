package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.texture.DayTimeTexture;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AnimationMetadataSection.class)
public class AnimationMetadataSectionMixin implements DayTimeTexture {
    @Unique
    private boolean polytone$usesWorldTime = false;

    @Override
    public boolean polytone$usesDayTime() {
        return polytone$usesWorldTime;
    }

    @Override
    public void polytone$setUsesDayTime(boolean usesWorldTime) {
        this.polytone$usesWorldTime = usesWorldTime;
    }
}
