package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.attributes.DynamicAttributeContext;
import net.minecraft.world.attribute.EnvironmentAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.attribute.EnvironmentAttributeProbe$ValueProbe")
public class ValueProbeMixin<Value> {

    @Shadow
    private Value lastValue;

    @Inject(method = "get", at = @At("HEAD"))
    private void polytone$clearTimeBlendRequest(EnvironmentAttribute<Value> attribute, float partialTicks,
                                                CallbackInfoReturnable<Value> cir) {
        DynamicAttributeContext.consumeTimeBlendRequest();
    }

    @ModifyExpressionValue(method = "get", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe$ValueProbe;getValueFromLevel(Lnet/minecraft/world/attribute/EnvironmentAttribute;)Ljava/lang/Object;"))
    private Value polytone$smoothOverTime(Value newValue, EnvironmentAttribute<Value> attribute, float partialTicks) {
        if (!DynamicAttributeContext.consumeTimeBlendRequest() || !attribute.isSpatiallyInterpolated()) return newValue;
        float seconds = Polytone.CONFIGS.attributeTimeSmoothing.get();
        if (seconds <= 0) return newValue;
        float alpha = (float) (1 - Math.exp(-0.05 / seconds));
        return attribute.type().partialTickLerp().apply(alpha, this.lastValue, newValue);
    }
}
