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
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;
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
                DynamicAttributes.addDynamicValueCodec(valueCodec, type);

        return Codec.either(supplierCodec, (Codec<EnvironmentAttributeMap.Entry<Value, Argument>>) (Codec) ec)
                .xmap(
                        EnvironmentAttributeMapMod::valueOrModToEntry,
                        EnvironmentAttributeMapMod::entryToValueOrMod
                );
    }

    private static <Value, Argument> @NotNull Either<Either<Argument, Supplier<Argument>>, EnvironmentAttributeMap.Entry<Value, Argument>> entryToValueOrMod(
            EnvironmentAttributeMap.Entry<Value, Argument> entry) {

        //an entry that opted out of blending has to keep the object form, that's where the flag lives
        if (entry.modifier() == AttributeModifier.override() && blendFromEntry(entry)) {
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
                        (AttributeModifier<Value, Argument>) AttributeModifier.override(), true),
                entry -> entry
        );
    }

    private static boolean blendFromEntry(EnvironmentAttributeMap.Entry<?, ?> entry) {
        return ((IExtendedEntry<?>) (Object) entry).polytone$shouldBlend();
    }

    private static <Value, Argument> EnvironmentAttributeMap.Entry<Value, Argument> entryFromSupplier(
            Either<Argument, Supplier<Argument>> valueOrSupplier, AttributeModifier<Value, Argument> modifier,
            boolean blend) {
        return valueOrSupplier.map(
                //gets default value from supplier. might be a bad idea
                value ->
                        new EnvironmentAttributeMap.Entry<>(
                                value,
                                modifier
                        ),
                supplier -> {
                    EnvironmentAttributeMap.Entry<Value, Argument> entry = new EnvironmentAttributeMap.Entry<>(supplier.get(), modifier);
                    ((IExtendedEntry) (Object) entry).polytone$setArgumentSupplier(supplier);
                    ((IExtendedEntry) (Object) entry).polytone$setShouldBlend(blend);
                    return entry;
                }
        );
    }

    // Copy of a dynamic entry pinned to one biome. Each biome an entry is installed into gets its own, so
    // vanilla's spatial interpolator sees a different value per biome and blends them like any other biome
    // attribute. Entries that opted out of blending are shared as they were and keep sampling at the camera.
    private static EnvironmentAttributeMap.Entry<?, ?> bindToBiome(EnvironmentAttributeMap.Entry<?, ?> entry,
                                                                  Biome owner) {
        IExtendedEntry ext = (IExtendedEntry) (Object) entry;
        Supplier<?> supplier = ext.polytone$getArgumentSupplier();
        if (supplier == null || !ext.polytone$shouldBlend()) return entry;

        EnvironmentAttributeMap.Entry bound = new EnvironmentAttributeMap.Entry(entry.argument(), entry.modifier());
        ((IExtendedEntry) (Object) bound).polytone$setArgumentSupplier(DynamicAttributes.boundTo(owner, supplier));
        return bound;
    }

    private static <Value, Argument> MapCodec<EnvironmentAttributeMap.Entry<Value, Argument>> createEntryCodec(
            EnvironmentAttribute<Value> environmentAttribute, AttributeModifier<Value, Argument> attributeModifier) {

        return RecordCodecBuilder.mapCodec((instance) ->
        {
            Codec<Argument> argumentCodec = attributeModifier.argumentCodec(environmentAttribute);
            Codec<Either<Argument, Supplier<Argument>>> argOrSupplier = DynamicAttributes.addDynamicValueCodec(argumentCodec, environmentAttribute.type());
            return instance.group(
                            argOrSupplier.fieldOf("argument")
                                    .forGetter(EnvironmentAttributeMapMod::supplierFromEntry),
                            Codec.BOOL.optionalFieldOf("blend", true)
                                    .forGetter(EnvironmentAttributeMapMod::blendFromEntry))
                    .apply(instance, (object, blend) ->
                            entryFromSupplier(object, attributeModifier, blend));
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entriesToReplace = new HashMap<>();
        private final Set<EnvironmentAttribute<?>> entriesToRemove = new HashSet<>();

        public <Value, Parameter> Builder modify(EnvironmentAttribute<Value> environmentAttribute,
                                                 AttributeModifier<Value, Parameter> attributeModifier,
                                                 Supplier<Parameter> objectSupplier) {
            environmentAttribute.type().checkAllowedModifier(attributeModifier);
            EnvironmentAttributeMap.Entry<Value, Parameter> entry = new EnvironmentAttributeMap.Entry<>(objectSupplier.get(), attributeModifier);
            ((IExtendedEntry<Parameter>) (Object) entry).polytone$setArgumentSupplier(objectSupplier);
            this.entriesToReplace.put(environmentAttribute, entry);
            return this;
        }

        public <Value, Parameter> Builder modify(EnvironmentAttribute<Value> environmentAttribute,
                                                 AttributeModifier<Value, Parameter> attributeModifier,
                                                 Parameter object) {
            environmentAttribute.type().checkAllowedModifier(attributeModifier);
            this.entriesToReplace.put(environmentAttribute, new EnvironmentAttributeMap.Entry<>(object, attributeModifier));
            return this;
        }

        public <Value> Builder set(EnvironmentAttribute<Value> environmentAttribute, Value object) {
            return this.modify(environmentAttribute, AttributeModifier.override(), object);
        }

        public <Value> Builder remove(EnvironmentAttribute<Value> attribute) {
            entriesToRemove.add(attribute);
            return this;
        }

        public EnvironmentAttributeMapMod build() {
            return new EnvironmentAttributeMapMod(entriesToReplace, entriesToRemove);
        }

        public boolean isEmpty() {
            return entriesToReplace.isEmpty() && entriesToRemove.isEmpty();
        }
    }

    public EnvironmentAttributeMap toVanilla() {
        return toVanilla(null);
    }

    // owner is the biome this map is being built for, if any. See bindToBiome
    public EnvironmentAttributeMap toVanilla(@Nullable Biome owner) {
        EnvironmentAttributeMap.Builder builder = EnvironmentAttributeMap.builder();
        putEntries(builder, owner);
        return builder.build();
    }

    private void putEntries(EnvironmentAttributeMap.Builder builder, @Nullable Biome owner) {
        if (owner == null) {
            builder.entries.putAll(entriesToReplace);
            return;
        }
        for (var e : entriesToReplace.entrySet()) {
            builder.entries.put(e.getKey(), bindToBiome(e.getValue(), owner));
        }
    }

    public Collection<EnvironmentAttribute<?>> getAlteredEntries() {
        return entriesToReplace.keySet();
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
        return modify(original, null);
    }

    // owner is the biome this map is being built for, if any. See bindToBiome
    public EnvironmentAttributeMap modify(EnvironmentAttributeMap original, @Nullable Biome owner) {
        if (isEmpty()) return original;
        EnvironmentAttributeMap.Builder builder = EnvironmentAttributeMap.builder();
        //add original entries except removed ones
        for (var key : original.keySet()) {
            if (!entriesToRemove.contains(key)) {
                builder.entries.put(key, original.get(key));
            }
        }
        //add new entries
        putEntries(builder, owner);
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
