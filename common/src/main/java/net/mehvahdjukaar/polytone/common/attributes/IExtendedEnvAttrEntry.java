package net.mehvahdjukaar.polytone.common.attributes;

import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.modifier.AttributeModifier;

import java.util.function.Supplier;

public interface IExtendedEnvAttrEntry<Value> {

    void polytone$setArgumentSupplier( Supplier<Value> supplier) ;

    Supplier<Value> polytone$getArgumentSupplier( );

    boolean polytone$shouldBlend();

    void polytone$setShouldBlend(boolean shouldBlend);

    default boolean polytone$isDynamic() {
        return polytone$getArgumentSupplier() != null;
    }

    @SuppressWarnings("unchecked")
    static <Argument> IExtendedEnvAttrEntry<Argument> of(EnvironmentAttributeMap.Entry<?, Argument> entry) {
        return (IExtendedEnvAttrEntry<Argument>) (Object) entry;
    }

    static <Value, Argument> EnvironmentAttributeMap.Entry<Value, Argument> createDynamic(
            Supplier<Argument> supplier, AttributeModifier<Value, Argument> modifier, boolean blend) {
        var entry = new EnvironmentAttributeMap.Entry<>(supplier.get(), modifier);
        IExtendedEnvAttrEntry<Argument> dynamic = of(entry);
        dynamic.polytone$setArgumentSupplier(supplier);
        dynamic.polytone$setShouldBlend(blend);
        return entry;
    }
}
