package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.common.attributes.DynamicAttributeContext;
import net.mehvahdjukaar.polytone.common.attributes.IExtendedEnvAttrEntry;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

@Mixin(EnvironmentAttributeMap.Entry.class)
public class EnvironmentAttributeEntryMixin<Value, Argument> implements IExtendedEnvAttrEntry<Value> {

    @Unique
    private Supplier<Value> polytone$argumentSupplier;

    @Unique
    private boolean polytone$shouldBlend = true;

    @Override
    public void polytone$setArgumentSupplier(Supplier<Value> supplier) {
        this.polytone$argumentSupplier = supplier;
    }

    @Override
    public Supplier<Value> polytone$getArgumentSupplier() {
        return this.polytone$argumentSupplier;
    }

    @Override
    public boolean polytone$shouldBlend() {
        return this.polytone$shouldBlend;
    }

    @Override
    public void polytone$setShouldBlend(boolean shouldBlend) {
        this.polytone$shouldBlend = shouldBlend;
    }

    @ModifyReturnValue(method = "argument", at = @At("RETURN"))
    private Value polytone$modifyArgumentReturnValue(Value original) {
        if (polytone$argumentSupplier != null) {
            return polytone$argumentSupplier.get();
        }
        return original;
    }

    @WrapOperation(method = "applyModifier", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/attribute/modifier/AttributeModifier;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Value polytone$applyWithDynamicArgument(AttributeModifier<?, ?> modifier, Object input, Object argument, Operation<Value> original) {
        if (polytone$argumentSupplier == null){
            return original.call(modifier, input, argument);
        }
        //incoming value is exposed to the expression as v
        return DynamicAttributeContext.withIncoming(input,
                () -> original.call(modifier, input, polytone$argumentSupplier.get()));
    }

}
