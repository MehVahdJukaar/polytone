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

    public static final Codec<EnvironmentAttributeMapMod> CODEC =
            Codec.lazyInitialized(() -> {
                        Codec<Map<EnvironmentAttribute<?>, Either<Removal, ? extends EnvironmentAttributeMap.Entry<?, ?>>>> mapCodec =
                                Codec.dispatchedMap(
                                        EnvironmentAttributes.CODEC,
                                        Util.memoize((EnvironmentAttribute<?> attr) ->
                                                Codec.either(Removal.CODEC, createEntryCodec(attr))
                                        )
                                );
                        return mapCodec.xmap(
                                EnvironmentAttributeMapMod::new,
                                mod -> mod.entriesToReplace.entrySet()
                                        .stream()
                                        .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                e -> Either.right(e.getValue())
                                        ))
                        );
                    }
            );


    private static <Value, Argument> Codec<EnvironmentAttributeMap.Entry<Value, Argument>> createEntryCodec(
            EnvironmentAttribute<Value> environmentAttribute) {

        AttributeType<Value> type = environmentAttribute.type();
        Codec<AttributeModifier<Value, ?>> attributeModifierCodec = type.modifierCodec();

        //generics are just wrong here... Hoping they line up at runtime
        Codec<EnvironmentAttributeMap.Entry<Value, ?>> ec =
                attributeModifierCodec.dispatch(
                        "modifier",
                        EnvironmentAttributeMap.Entry::modifier,
                        Util.memoize(mod -> createEntryCodec(environmentAttribute, mod))
                );

        Codec<Argument> valueCodec = (Codec) environmentAttribute.valueCodec();
        Codec<Either<Argument, Supplier<Argument>>> supplierCodec =
                ExtendedAttributeMod.addDynamicValueCodec(valueCodec, type);

        return Codec.either(supplierCodec, (Codec<EnvironmentAttributeMap.Entry<Value, Argument>>) (Codec) ec)
                .xmap(
                        EnvironmentAttributeMapMod::valueOrModToEntry,
                        EnvironmentAttributeMapMod::entryToValueOrMod
                );
    }


    private static <Value, Argument> @NotNull Either<Either<Argument, Supplier<Argument>>, EnvironmentAttributeMap.Entry<Value, Argument>> entryToValueOrMod(
            EnvironmentAttributeMap.Entry<Value, Argument> entry) {

        if (entry.modifier() == AttributeModifier.override()) {
            Either<Argument, Supplier<Argument>> valOrSup = supplierFromEntry(entry);
            return Either.left(valOrSup);
        }

        return Either.right(entry);
    }

    private static <Value, Argument> @NotNull Either<Argument, Supplier<Argument>> supplierFromEntry(
            EnvironmentAttributeMap.Entry<Value, Argument> entry) {
        Either<Argument, Supplier<Argument>> valOrSup;
        Supplier<Argument> argSupp = ((IExtendedEntry<Argument>) (Object) entry).polytone$getArgumentSupplier();
        if (argSupp != null) {
            valOrSup = Either.right(argSupp);
        } else {
            Argument arg = entry.argument();
            valOrSup = Either.left(arg);
        }
        return valOrSup;
    }

    private static <Value, Argument> EnvironmentAttributeMap.Entry<Value, Argument> valueOrModToEntry(
            Either<Either<Argument, Supplier<Argument>>, EnvironmentAttributeMap.Entry<Value, Argument>> either) {

        return either.map(
                a -> EnvironmentAttributeMapMod.entryFromSupplier(a,
                        (AttributeModifier<Value, Argument>) AttributeModifier.override()),
                entry -> entry
        );
    }

    private static <Value, Argument> EnvironmentAttributeMap.Entry<Value, Argument> entryFromSupplier(
            Either<Argument, Supplier<Argument>> valueOrSupplier, AttributeModifier<Value, Argument> modifier) {
        return valueOrSupplier.map(
                value ->
                        new EnvironmentAttributeMap.Entry<>(
                                value,
                                modifier
                        ),
                supplier -> {
                    EnvironmentAttributeMap.Entry<Value, Argument> entry = new EnvironmentAttributeMap.Entry<>(supplier.get(), modifier);
                    ((IExtendedEntry) (Object) entry).polytone$setArgumentSupplier(supplier);
                    return entry;
                }
        );
    }


    private static <Value, Argument> MapCodec<EnvironmentAttributeMap.Entry<Value, Argument>> createEntryCodec(
            EnvironmentAttribute<Value> environmentAttribute, AttributeModifier<Value, Argument> attributeModifier) {

        return RecordCodecBuilder.mapCodec((instance) ->
        {
            Codec<Argument> argumentCodec = attributeModifier.argumentCodec(environmentAttribute);
            Codec<Either<Argument, Supplier<Argument>>> argOrSupplier = ExtendedAttributeMod.addDynamicValueCodec(argumentCodec, environmentAttribute.type());
            return instance.group(argOrSupplier.fieldOf("argument")
                            .forGetter(EnvironmentAttributeMapMod::supplierFromEntry))
                    .apply(instance, (object) ->
                            entryFromSupplier(object, attributeModifier));
        });
    }

    private EnvironmentAttributeMapMod(Map<EnvironmentAttribute<?>,
            Either<Removal, ? extends EnvironmentAttributeMap.Entry<?, ?>>> entries) {
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

    public EnvironmentAttributeMap toVanilla() {
        EnvironmentAttributeMap.Builder builder = EnvironmentAttributeMap.builder();
        builder.entries.putAll(entriesToReplace);
        return builder.build();
    }


    public boolean isEmpty() {
        return entriesToReplace.isEmpty() && entriesToRemove.isEmpty();
    }

    public static EnvironmentAttributeMapMod wrapVanilla(EnvironmentAttributeMap attributes) {
        if (attributes == EnvironmentAttributeMap.EMPTY) return EMPTY;
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
