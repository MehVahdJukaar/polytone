package net.mehvahdjukaar.polytone.common.attributes;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Supplier;

public class EnvironmentAttributeMapMod {

    public static final EnvironmentAttributeMapMod EMPTY = new EnvironmentAttributeMapMod(Map.of(), Set.of());

    public static final Codec<EnvironmentAttributeMapMod> CODEC = Codec.lazyInitialized(() -> {
        Codec<Map<EnvironmentAttribute<?>, Either<Removal, ? extends EnvironmentAttributeMap.Entry<?, ?>>>> mapCodec =
                Codec.dispatchedMap(
                        EnvironmentAttributes.CODEC,
                        Util.memoize((EnvironmentAttribute<?> attr) ->
                                Codec.either(Removal.CODEC, EnvAttrEntryCodecs.entryCodec(attr)))
                );
        return mapCodec.xmap(EnvironmentAttributeMapMod::fromEntriesOrRemovals, EnvironmentAttributeMapMod::toEntriesOrRemovals);
    });

    private final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entriesToReplace;
    private final Set<EnvironmentAttribute<?>> entriesToRemove;

    private EnvironmentAttributeMapMod(Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entriesToReplace,
                                       Set<EnvironmentAttribute<?>> entriesToRemove) {
        this.entriesToReplace = entriesToReplace;
        this.entriesToRemove = entriesToRemove;
    }

    private static EnvironmentAttributeMapMod fromEntriesOrRemovals(
            Map<EnvironmentAttribute<?>, Either<Removal, ? extends EnvironmentAttributeMap.Entry<?, ?>>> parsed) {
        Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> toReplace = new HashMap<>();
        Set<EnvironmentAttribute<?>> toRemove = new HashSet<>();
        for (var e : parsed.entrySet()) {
            Optional<? extends EnvironmentAttributeMap.Entry<?, ?>> entry = e.getValue().right();
            if (entry.isPresent()) toReplace.put(e.getKey(), entry.get());
            else toRemove.add(e.getKey());
        }
        return new EnvironmentAttributeMapMod(toReplace, toRemove);
    }

    private static Map<EnvironmentAttribute<?>, Either<Removal, ? extends EnvironmentAttributeMap.Entry<?, ?>>> toEntriesOrRemovals(
            EnvironmentAttributeMapMod mod) {
        Map<EnvironmentAttribute<?>, Either<Removal, ? extends EnvironmentAttributeMap.Entry<?, ?>>> map = new HashMap<>();
        for (var e : mod.entriesToReplace.entrySet()) {
            map.put(e.getKey(), Either.right(e.getValue()));
        }
        for (var attribute : mod.entriesToRemove) {
            map.put(attribute, Either.left(Removal.UNIT));
        }
        return map;
    }

    public static EnvironmentAttributeMapMod wrapVanilla(EnvironmentAttributeMap attributes) {
        if (attributes == EnvironmentAttributeMap.EMPTY) return EMPTY;
        return new EnvironmentAttributeMapMod(EnvironmentAttributeMap.builder().putAll(attributes).entries, Set.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return entriesToReplace.isEmpty() && entriesToRemove.isEmpty();
    }

    public Collection<EnvironmentAttribute<?>> getAlteredEntries() {
        return entriesToReplace.keySet();
    }

    public @Nullable EnvironmentAttributeMap.Entry<?, ?> getEntry(EnvironmentAttribute<?> attribute) {
        return entriesToReplace.get(attribute);
    }

    public boolean removes(EnvironmentAttribute<?> attribute) {
        return entriesToRemove.contains(attribute);
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
        for (var key : original.keySet()) {
            if (!entriesToRemove.contains(key)) {
                builder.entries.put(key, original.get(key));
            }
        }
        putReplacements(builder, owner);
        return builder.build();
    }

    public EnvironmentAttributeMap toVanilla() {
        return toVanilla(null);
    }

    public EnvironmentAttributeMap toVanilla(@Nullable Biome owner) {
        EnvironmentAttributeMap.Builder builder = EnvironmentAttributeMap.builder();
        putReplacements(builder, owner);
        return builder.build();
    }

    private void putReplacements(EnvironmentAttributeMap.Builder builder, @Nullable Biome owner) {
        if (owner == null) {
            builder.entries.putAll(entriesToReplace);
            return;
        }
        for (var e : entriesToReplace.entrySet()) {
            builder.entries.put(e.getKey(), bindToBiome(e.getValue(), owner));
        }
    }

    // biome entries get one bound copy per targeted biome so vanilla's interpolator lerps between them
    private static <Argument> EnvironmentAttributeMap.Entry<?, Argument> bindToBiome(EnvironmentAttributeMap.Entry<?, Argument> entry,
                                                                                    Biome owner) {
        IExtendedEnvAttrEntry<Argument> ext = IExtendedEnvAttrEntry.of(entry);
        if (!ext.polytone$isDynamic() || !ext.polytone$getBlend().biome()) return entry;

        Supplier<Argument> supplier = ext.polytone$getArgumentSupplier();
        return IExtendedEnvAttrEntry.createDynamic(() -> DynamicAttributeContext.inBiome(owner, supplier),
                entry.modifier(), ext.polytone$getBlend());
    }

    public static class Builder {
        private final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entriesToReplace = new HashMap<>();
        private final Set<EnvironmentAttribute<?>> entriesToRemove = new HashSet<>();

        public <Value, Parameter> Builder modify(EnvironmentAttribute<Value> environmentAttribute,
                                                 AttributeModifier<Value, Parameter> attributeModifier,
                                                 Supplier<Parameter> objectSupplier) {
            environmentAttribute.type().checkAllowedModifier(attributeModifier);
            this.entriesToReplace.put(environmentAttribute, IExtendedEnvAttrEntry.createDynamic(objectSupplier, attributeModifier, IExtendedEnvAttrEntry.Blend.DEFAULT));
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

    private enum Removal implements StringRepresentable {
        UNIT;
        public static final Codec<Removal> CODEC = StringRepresentable.fromEnum(Removal::values);

        @Override
        public @NonNull String getSerializedName() {
            return "REMOVE";
        }
    }
}
