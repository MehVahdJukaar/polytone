package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.common.attributes.IExtendedEntry;
import net.minecraft.world.attribute.*;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
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
        if ((Object) entry instanceof IExtendedEntry pe && pe.polytone$getArgumentSupplier() != null) {
            var builder = this.addPositionalLayer(attribute, new EnvironmentAttributeLayer.Positional<Value>() {
                @Override
                public Value applyPositional(Value oldValue, Vec3 vec3, @Nullable SpatialAttributeInterpolator spatialAttributeInterpolator) {
                    return entry.applyModifier(oldValue);
                }
            });
            cir.setReturnValue(builder);
        }
    }
}
