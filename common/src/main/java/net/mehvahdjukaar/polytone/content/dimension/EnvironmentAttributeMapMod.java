package net.mehvahdjukaar.polytone.content.dimension;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnvironmentAttributeMapMod {
    private final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entriesToReplace;
    private final Set<EnvironmentAttribute<?>> entriesToRemove;

    public static final EnvironmentAttributeMapMod EMPTY = new EnvironmentAttributeMapMod(Map.of());
    @SuppressWarnings("unchecked")
    public static final Codec<EnvironmentAttributeMapMod> CODEC = Codec.lazyInitialized(
            () -> Codec.dispatchedMap(
                    EnvironmentAttributes.CODEC,
                    Util.memoize((EnvironmentAttribute<?> attr) ->
                            (Codec<Either<Removal, EnvironmentAttributeMap.Entry<?, ?>>>) (Object)
                                    Codec.either(Removal.CODEC, EnvironmentAttributeMap.Entry.createCodec(attr))
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
        EnvironmentAttributeMap.Builder builder = EnvironmentAttributeMap.builder();
        //add original entries except removed ones
        for (var key : original.keySet()) {
            if (!entriesToRemove.contains(key)) {
                builder.set(key, original.get(key));
            }
        }
        //add new entries
        builder.entries.putAll(entriesToReplace);
        return builder.build();
    }


    private enum Removal {
        UNIT;
        public static final Codec<Removal> CODEC = MapCodec.unitCodec(UNIT);
    }

}
