package net.mehvahdjukaar.polytone.common.attributes;

import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.modifier.AttributeModifier;

import java.util.function.Supplier;

public interface IExtendedEnvAttrEntry<Value> {

    record Blend(boolean biome, boolean time) {
        public static final Blend DEFAULT = new Blend(true, false);
    }

    void polytone$setArgumentSupplier( Supplier<Value> supplier) ;

    Supplier<Value> polytone$getArgumentSupplier( );

    Blend polytone$getBlend();

    void polytone$setBlend(Blend blend);

    default boolean polytone$isDynamic() {
        return polytone$getArgumentSupplier() != null;
    }

    @SuppressWarnings("unchecked")
    static <Argument> IExtendedEnvAttrEntry<Argument> of(EnvironmentAttributeMap.Entry<?, Argument> entry) {
        return (IExtendedEnvAttrEntry<Argument>) (Object) entry;
    }

    static <Value, Argument> EnvironmentAttributeMap.Entry<Value, Argument> createDynamic(
            Supplier<Argument> supplier, AttributeModifier<Value, Argument> modifier, Blend blend) {
        var entry = new EnvironmentAttributeMap.Entry<>(supplier.get(), modifier);
        IExtendedEnvAttrEntry<Argument> dynamic = of(entry);
        dynamic.polytone$setArgumentSupplier(supplier);
        dynamic.polytone$setBlend(blend);
        return entry;
    }
}
