package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.common.attributes.DynamicAttributeContext;
import net.mehvahdjukaar.polytone.common.attributes.IExtendedEnvAttrEntry;
import net.minecraft.world.attribute.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnvironmentAttributeSystem.Builder.class)
public abstract class EnvironmentAttributeSystemBuilderMixin {

    @Shadow
    public abstract <Value> EnvironmentAttributeSystem.Builder addPositionalLayer(EnvironmentAttribute<Value> environmentAttribute, EnvironmentAttributeLayer.Positional<Value> positional);

    @Inject(method = "addConstantEntry", at = @At("HEAD"), cancellable = true)
    private <Value> void polytone$turnConstantIntoPositional(EnvironmentAttribute<Value> attribute,
                                                             EnvironmentAttributeMap attributeMap,
                                                             CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> cir) {
        EnvironmentAttributeMap.Entry<Value, ?> entry = attributeMap.get(attribute);
        if ((Object) entry instanceof IExtendedEnvAttrEntry<?> dynamic && dynamic.polytone$isDynamic()) {
            //lets the probe know it has to record biome weights for us
            DynamicAttributeContext.hasDynamicLayers = true;

            boolean blend = dynamic.polytone$shouldBlend();
            cir.setReturnValue(this.addPositionalLayer(attribute, (oldValue, pos, interpolator) -> blend
                    ? DynamicAttributeContext.applyBlended(attribute, entry, oldValue, interpolator)
                    : entry.applyModifier(oldValue)));
        }
    }
}
