package net.mehvahdjukaar.polytone.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface ITargetProvider {

    Codec<Set<ResourceLocation>> TARGET_CODEC = Codec.either(ResourceLocation.CODEC,
            ResourceLocation.CODEC.listOf()).xmap(e -> e.map(Set::of, HashSet::new), s -> Either.right(new ArrayList<>(s)));


    default <T> Set<T> mergeSet(Set<T> first, Set<T> second) {
        var set = new HashSet<T>();
        set.addAll(first);
        set.addAll(second);
        return Collections.unmodifiableSet(set);
    }

    default <T> List<T> mergeList(List<T> first, List<T> second) {
        var list = new ArrayList<T>();
        list.addAll(first);
        list.addAll(second);
        return Collections.unmodifiableList(list);
    }


    @NotNull
    Set<ResourceLocation> explicitTargets();

    default Set<ResourceLocation> getTargetsKeys(ResourceLocation fileId) {
        var expl = this.explicitTargets();
        if (expl.isEmpty()) return Set.of(fileId);
        return expl;
    }

    default <T> Set<T> getTargets(ResourceLocation fileId, Registry<T> registry) {
        Set<T> set = new HashSet<>();
        var explTargets = this.explicitTargets();
        Optional<T> implicitTarget = registry.getOptional(fileId);
        if (!explTargets.isEmpty()) {
            if (implicitTarget.isPresent() && !explTargets.contains(fileId)) {
                Polytone.LOGGER.error("Found Polytone file with explicit Targets ({}) also having a valid IMPLICIT (file path) Target ({})." +
                        "Consider moving it under your OWN namespace to avoid overriding other packs modifiers with the same path", explTargets, fileId);
            }
            for (var explicitId : explTargets) {
                Optional<T> target = registry.getOptional(explicitId);
                target.ifPresent(set::add);
            }
        }
        //no explicit targets. use its own ID instead
        else {
            if (implicitTarget.isPresent()) {
                set.add(implicitTarget.get());
            } else {
                Polytone.LOGGER.error("Found Polytone file {} with no valid implicit target and no explicit targets from registry {}", fileId, registry);
            }
        }
        return set;
    }

}
