package net.mehvahdjukaar.polytone.common.attributes;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class EnvironmentAttributeMapMod {
    private final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entriesToReplace;
    private final Set<EnvironmentAttribute<?>> entriesToRemove;

    public static final EnvironmentAttributeMapMod EMPTY = new EnvironmentAttributeMapMod(Map.of());

    public static final Codec<EnvironmentAttributeMapMod> CODEC = Codec.lazyInitialized(
            () -> Codec.dispatchedMap(
                    EnvironmentAttributes.CODEC,
                    Util.memoize((EnvironmentAttribute<?> attr) -> Codec.either(Removal.CODEC, createEntryCodec(attr))
                    )
            ).xmap(EnvironmentAttributeMapMod::new,
                    mod -> mod.entriesToReplace.entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> Either.right(e.getValue())
                                    )
                            )
            ));

    @SuppressWarnings("unchecked")
    private static <Value> Codec<EnvironmentAttributeMap.Entry<?, ?>> createEntryCodec(EnvironmentAttribute<Value> environmentAttribute) {
        AttributeType<Value> type = environmentAttribute.type();
        Codec<AttributeModifier<Value, ?>> attributeModifierCodec = type.modifierCodec();
        Codec<EnvironmentAttributeMap.Entry<Value, ?>> codec = attributeModifierCodec.dispatch("modifier", EnvironmentAttributeMap.Entry::modifier,
                Util.memoize((attributeModifier) ->
                        createFullCodec(environmentAttribute, attributeModifier)));
        Codec<Value> valueCodec = environmentAttribute.valueCodec();
        Codec<Either<Value, Supplier<Value>>> supplierCodec = ExtendedAttributeMod.addDynamicValueCodec(valueCodec, type);

        return Codec.either(supplierCodec, codec)
                .xmap(
                        EnvironmentAttributeMapMod::valueOrModToEntry,
                        EnvironmentAttributeMapMod::entryToValueOrMod
                );
    }

    private static @NotNull Either<Either<?, Supplier<?>>, EnvironmentAttributeMap.Entry<?, ?>>
    entryToValueOrMod(EnvironmentAttributeMap.Entry<?, ?> entry) {

        if (entry.modifier() == AttributeModifier.override()) {
            Supplier argSupp = ((IExtendedEntry) (Object) entry).polytone$getArgumentSupplier();
            if (argSupp != null) {
                return Either.left(Either.right(argSupp));
            } else {
                return Either.left(Either.left(entry.argument()));
            }
        }

        return Either.right(entry);
    }

    private static <Value> EnvironmentAttributeMap.Entry<Value, ?> valueOrModToEntry(
            Either<Either<Value, Supplier<Value>>,
            EnvironmentAttributeMap.Entry<Value, ?>> either) {

        return either.map(
                valueOrSupplier -> valueOrSupplier.map(
                        value -> new EnvironmentAttributeMap.Entry<>(value, AttributeModifier.override()),
                        supplier -> createWithSupplier(
                                supplier, AttributeModifier.override()
                        )
                ),
                entry -> entry
        );
    }

    private static <Value> EnvironmentAttributeMap.Entry<Value,?> createWithSupplier(
            Supplier<Value> argumentSupplier, AttributeModifier<?, ?> modifier) {
        return new EnvironmentAttributeMap.Entry<>(argumentSupplier.get(), modifier);
    }

    //extended argument to take expressions and colormaps
    private static <Value, Argument> MapCodec<EnvironmentAttributeMap.Entry<Value, Argument>> createFullCodec(EnvironmentAttribute<Value> environmentAttribute, AttributeModifier<Value, Argument> attributeModifier) {
        return RecordCodecBuilder.mapCodec((instance) ->
        {
            Codec<Argument> argumentCodec = attributeModifier.argumentCodec(environmentAttribute);
            argumentCodec = ExtendedAttributeMod.addDynamicValueCodec(argumentCodec, environmentAttribute.type());
            return instance.group(argumentCodec.fieldOf("argument")
                            .forGetter(EnvironmentAttributeMap.Entry::argument))
                    .apply(instance, (object) -> new EnvironmentAttributeMap.Entry<>(object, attributeModifier));
        });
    }

    private EnvironmentAttributeMapMod(Map<EnvironmentAttribute<?>,
            Either<Removal, EnvironmentAttributeMap.Entry<?, ?>>> entries) {
        this.entriesToReplace = entries.entrySet().stream()
                .filter(e -> e.getValue().right().isPresent())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().right().get()
                ));
        this.entriesToRemove = entries.entrySet().stream()
                .filter(e -> e.getValue().left().isPresent())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

    }

    private EnvironmentAttributeMapMod(Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entriesToAdd,
                                       Set<EnvironmentAttribute<?>> entriesToRemove) {
        this.entriesToReplace = entriesToAdd;
        this.entriesToRemove = entriesToRemove;
    }

    public EnvironmentAttributeMap toVanilla(){
        EnvironmentAttributeMap.Builder builder = EnvironmentAttributeMap.builder();
        builder.entries.putAll(entriesToReplace);
        return builder.build();
    }


    public boolean isEmpty() {
        return entriesToReplace.isEmpty() && entriesToRemove.isEmpty();
    }

    public static EnvironmentAttributeMapMod wrapVanilla(EnvironmentAttributeMap attributes) {
        return new EnvironmentAttributeMapMod(EnvironmentAttributeMap.builder().putAll(attributes).entries, Set.of());
    }

    public EnvironmentAttributeMapMod merge(EnvironmentAttributeMapMod newMod) {
        Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> mergedEntriesToAdd = new HashMap<>(this.entriesToReplace);
        mergedEntriesToAdd.putAll(newMod.entriesToReplace);

        Set<EnvironmentAttribute<?>> mergedEntriesToRemove = new HashSet<>(this.entriesToRemove);
        mergedEntriesToRemove.addAll(newMod.entriesToRemove);

        return new EnvironmentAttributeMapMod(mergedEntriesToAdd, mergedEntriesToRemove);
    }

    public EnvironmentAttributeMap modify(EnvironmentAttributeMap original) {
        if (isEmpty()) return original;
        EnvironmentAttributeMap.Builder builder = EnvironmentAttributeMap.builder();
        //add original entries except removed ones
        for (var key : original.keySet()) {
            if (!entriesToRemove.contains(key)) {
                builder.entries.put(key, original.get(key));
            }
        }
        //add new entries
        builder.entries.putAll(entriesToReplace);
        return builder.build();
    }


    private enum Removal implements StringRepresentable {
        UNIT;
        public static final Codec<Removal> CODEC = StringRepresentable.fromEnum(Removal::values);

        @Override
        public @NonNull String getSerializedName() {
            return "REMOVE";
        }
    }

}
