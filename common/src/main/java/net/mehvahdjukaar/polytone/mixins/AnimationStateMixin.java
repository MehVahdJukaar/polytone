package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.content.texture.IDayTimeContext;
import net.mehvahdjukaar.polytone.misc.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteContents.AnimationState.class)
public class AnimationStateMixin implements IDayTimeContext {

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
    }

    @Override
    public void polytone$setTimeCycleDuration(int duration) {
        this.polytone$dayDuration = duration;
    }

    @Override
    public int polytone$getTimeCycleDuration() {
        return polytone$dayDuration;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void polytone$overrideTick(CallbackInfo ci) {
        if (this.polytone$mode != Mode.VANILLA) {
            // Cancel vanilla frame advancement
            ci.cancel();

            // Compute frame/subFrame based on our time source
            polytone$updateFramesFromTime();
        }
    }

    @Shadow
    @Mutable
    private int frame;
    @Shadow @Mutable private int subFrame;

    @Shadow
    @Final
    private SpriteContents.AnimatedTexture animationInfo;

    @Unique
    private void polytone$updateFramesFromTime() {
        // Step 1: get current delta in [0,1)
        Float delta = polytone$mode.getDelta(polytone$dayDuration); // implement based on polytone$mode

        if (delta == null || delta < 0) return; // fallback if world unavailable

        // Step 2: compute total duration
        SpriteContents.AnimatedTexture anim = this.animationInfo;
        int totalTime = anim.frames.stream().mapToInt(f -> f.time).sum();

        // Step 3: compute cumulative frame times
        int accumulated = 0;
        for (int i = 0; i < anim.frames.size(); i++) {
            SpriteContents.FrameInfo frameInfo = anim.frames.get(i);
            int scaledTime = (int)(frameInfo.time * delta * totalTime);
            if (scaledTime < accumulated + frameInfo.time) {
                // Found current frame
                frame = i;
                subFrame = (scaledTime - accumulated);
                break;
            }
            accumulated += frameInfo.time;
        }
    }

}
