package net.mehvahdjukaar.polytone.mixins;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.mehvahdjukaar.polytone.texture.DayTimeTexture;
import net.mehvahdjukaar.polytone.texture.PolytoneTextureTicker;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteContents.AnimatedTexture.class)
public abstract class AnimatedTextureMixin implements DayTimeTexture {

    @Shadow
    @Final
    private boolean interpolateFrames;
    @Shadow
    @Final
    SpriteContents field_28469;

    @Shadow
    public abstract SpriteContents.AnimationState createAnimationState(GpuBufferSlice gpuBufferSlice, int m);

    @Unique
    private Mode polytone$mode = Mode.VANILLA;
    @Unique
    private int polytone$dayDuration = 0;

    @Override
    public Mode polytone$getMode() {
        return polytone$mode;
    }

    @Override
    public void polytone$setMode(Mode mode) {
        this.polytone$mode = mode;
        if (mode == Mode.DAY_TIME) {
            polytone$dayDuration = SharedConstants.TICKS_PER_GAME_DAY;
        } else if (mode == Mode.GAME_TIME) {
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

    /* FIXME - use createanimationstate */

    @Inject(method = "createAnimationState", at = @At("HEAD"), cancellable = true)
    public void polytone$modifyTicker(CallbackInfoReturnable<SpriteTicker> cir) {
        if (polytone$mode != Mode.VANILLA) {

            var t = new PolytoneTextureTicker((SpriteContents.AnimatedTexture) (Object) this, this.field_28469,
                    this.interpolateFrames, this.polytone$dayDuration, this.polytone$mode);
            cir.setReturnValue(t);
        }
    }
}
