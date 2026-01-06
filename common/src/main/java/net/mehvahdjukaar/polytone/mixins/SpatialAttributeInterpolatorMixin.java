package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.common.attributes.IExtendedInterpolator;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpatialAttributeInterpolator.class)
public class SpatialAttributeInterpolatorMixin implements IExtendedInterpolator {

    @Unique
    private SpatialAttributeInterpolator poly$postInterpolator = null;


    @Override
    public SpatialAttributeInterpolator polytone$getOrCreatePostInterpolator() {
        if (poly$postInterpolator == null) {
            poly$postInterpolator = new SpatialAttributeInterpolator();
        }
        return poly$postInterpolator;
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void polytone$clearPostInterpolator(CallbackInfo ci) {
        if (poly$postInterpolator != null) {
            poly$postInterpolator.clear();
        }
    }
}
