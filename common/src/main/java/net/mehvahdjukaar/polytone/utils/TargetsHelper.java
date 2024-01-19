package net.mehvahdjukaar.polytone.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TargetsHelper {

    public static final Codec<Set<ResourceLocation>> CODEC = Codec.either(ResourceLocation.CODEC,
            ResourceLocation.CODEC.listOf()).xmap(e -> e.map(Set::of, HashSet::new), s -> Either.right(new ArrayList<>(s)));


    public static Optional<Set<ResourceLocation>> merge(Optional<Set<ResourceLocation>> first, Optional<Set<ResourceLocation>> second) {
        var set = new HashSet<ResourceLocation>();
        first.ifPresent(set::addAll);
        second.ifPresent(set::addAll);
        return set.isEmpty() ? Optional.empty() : Optional.of(set);
    }

    //unused now
    public static ResourceLocation getLocalId(ResourceLocation path) {
        //TODO: this is unconventional and bad
        return new ResourceLocation(path.getPath().replaceFirst("/", ":"));
    }

    // gets a target either at local path or global one
    @Nullable
    public static <T> Pair<T, ResourceLocation> getTarget(ResourceLocation resourcePath, Registry<T> registry) {
        ResourceLocation id = getLocalId(resourcePath);
        // var opt = registry.getOptional(id);
        //if (opt.isPresent()) return Pair.of(opt.get(), id);
        var opt = registry.getOptional(resourcePath);
        return opt.map(t -> Pair.of(t, resourcePath)).orElse(null);
    }
}
