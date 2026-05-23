package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.attributes.IExtendedInterpolator;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnvironmentAttributeProbe.class)
public class AttributeProbeMixin {

    @Shadow
    @Final
    private SpatialAttributeInterpolator biomeInterpolator;

    @Inject(method = {"lambda$tick$0","method_75687"}, at = @At("HEAD"))
    private void poly$accumulateInner(double weight, Holder<Biome> holder, CallbackInfo ci) {
        if (Polytone.BIOME_MODIFIERS.hasPostAttributes()) {
            SpatialAttributeInterpolator postInterpolator = ((IExtendedInterpolator) this.biomeInterpolator)
                    .polytone$getOrCreatePostInterpolator();
            if (postInterpolator != null) {
                postInterpolator.accumulate(weight, Polytone.BIOME_MODIFIERS.getPostAttributes(holder.value()));
            }
        }
    }
}
