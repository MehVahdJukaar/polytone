package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.common.attributes.IExtendedEntry;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Supplier;

@Mixin(EnvironmentAttributeMap.Entry.class)
public class EnvironmentAttributeEntryMixin<Value, Argument> implements IExtendedEntry<Value> {

    @Unique
    private Supplier<Value> polytone$argumentSupplier;

    @Override
    public void polytone$setArgumentSupplier(Supplier<Value> supplier) {
        this.polytone$argumentSupplier = supplier;
    }

    @Override
    public Supplier<Value> polytone$getArgumentSupplier() {
        return this.polytone$argumentSupplier;
    }

    @ModifyReturnValue(method = "argument", at = @At("RETURN"))
    private Value polytone$modifyArgumentReturnValue(Value original) {
        if (polytone$argumentSupplier != null) {
            return polytone$argumentSupplier.get();
        }
        return original;
    }

    @ModifyArg(method = "applyModifier", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/attribute/modifier/AttributeModifier;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            ordinal = 0))
    private Value polytone$modifyApplyModifierArg(Value original) {
        if (polytone$argumentSupplier != null) {
            return polytone$argumentSupplier.get();
        }
        return original;
    }

}
