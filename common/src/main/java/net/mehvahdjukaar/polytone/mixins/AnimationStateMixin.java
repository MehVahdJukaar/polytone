package net.mehvahdjukaar.polytone.mixins;

import com.google.common.base.Preconditions;
import net.mehvahdjukaar.polytone.content.texture.IDeltaProvider;
import net.mehvahdjukaar.polytone.content.texture.IDeltaProviderContext;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteContents.AnimationState.class)
public class AnimationStateMixin implements IDeltaProviderContext {

    @Shadow
    private boolean isDirty;
    @Unique
    private IDeltaProvider polytone$mode = IDeltaProvider.PresetProvider.VANILLA;
    @Unique
    private int polytone$dayDuration = 0;

    @Override
    public IDeltaProvider polytone$getDeltaProvider() {
        return polytone$mode;
    }

    @Override
    public void polytone$setDeltaProvider(@NotNull IDeltaProvider mode) {
        Preconditions.checkNotNull(mode);
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
        if (this.polytone$mode != IDeltaProvider.PresetProvider.VANILLA) {
            // Cancel vanilla frame advancement
            ci.cancel();

            Float delta = polytone$mode.getDelta(polytone$dayDuration);
            if (delta == null || delta < 0) return;

            SpriteContents.AnimatedTexture anim = this.animationInfo;
            int totalTime = anim.frames.stream().mapToInt(SpriteContents.FrameInfo::time).sum();
            float scaledTime = delta * totalTime;

            int accumulated = 0;
            int oldFrame = this.frame; // Store old frame to check for changes
            int oldSubFrame = this.subFrame;

            for (int i = 0; i < anim.frames.size(); i++) {
                SpriteContents.FrameInfo frameInfo = anim.frames.get(i);
                if (scaledTime < accumulated + frameInfo.time()) {
                    this.frame = i;
                    this.subFrame = (int)(scaledTime - accumulated);
                    break;
                }
                accumulated += frameInfo.time();
            }

            // Update isDirty if frame changed (mimics vanilla behavior)
            if (this.frame != oldFrame || this.subFrame != oldSubFrame) {
                this.isDirty = true;
            }
        }
    }

    @Shadow
    @Mutable
    private int frame;
    @Shadow @Mutable private int subFrame;

    @Shadow
    @Final
    private SpriteContents.AnimatedTexture animationInfo;

}
