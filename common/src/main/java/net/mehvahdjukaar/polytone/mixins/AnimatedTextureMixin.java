package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.texture.DayTimeTexture;
import net.mehvahdjukaar.polytone.texture.DayTimeTextureTicker;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
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

    @Inject(method = "createTicker", at = @At("HEAD"), cancellable = true)
    public void polytone$modifyTicker(CallbackInfoReturnable<SpriteTicker> cir) {
        if (polytone$usesWorldTime) {
            var t = new DayTimeTextureTicker((SpriteContents.AnimatedTexture) (Object) this, this.field_28469, this.interpolateFrames);
            cir.setReturnValue(t);
        }
    }
}
