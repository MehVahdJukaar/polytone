package net.mehvahdjukaar.polytone.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface ITargetProvider {

    String WILDCARD_PLACEHOLDER = "all";
    ResourceLocation ALL_WILDCARD = Polytone.res(WILDCARD_PLACEHOLDER);
    Pattern WILDCARD_PATTERN = Pattern.compile("(^.*):\\*");

    Codec<ResourceLocation> WILDCARD_CODEC = Codec.STRING.flatXmap(s -> {
        Matcher matcher = WILDCARD_PATTERN.matcher(s);
        if (matcher.matches()) {
            String group = matcher.group(1);
            if (group != null)
                return DataResult.success(
                        new ResourceLocation(group, WILDCARD_PLACEHOLDER));
        }
        if (s.equals("*")) return DataResult.success(ALL_WILDCARD);
        return DataResult.error(() -> "Wildcard target must be '*'. Was: " + s);
    }, s -> DataResult.success(s.toString()));

    Codec<ResourceLocation> WILDCARD_OR_RES = CodecUtil.withAlternative(ResourceLocation.CODEC, WILDCARD_CODEC);

    Codec<Set<ResourceLocation>> TARGET_CODEC = CodecUtil.withAlternative(WILDCARD_OR_RES.listOf(), WILDCARD_OR_RES,
            List::of).xmap(Set::copyOf, List::copyOf);


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
        Set<ResourceLocation> explTargets = this.explicitTargets();
        Optional<T> implicitTarget = registry.getOptional(fileId);
        if (!explTargets.isEmpty()) {
            if (implicitTarget.isPresent() && !explTargets.contains(fileId)) {
                Polytone.LOGGER.error("Found Polytone file with explicit Targets ({}) also having a valid IMPLICIT (file path) Target ({})." +
                        "Consider moving it under your OWN namespace to avoid overriding other packs modifiers with the same path", explTargets, fileId);
            }
            for (var explicitId : explTargets) {

                if (explicitId.getPath().equals(WILDCARD_PLACEHOLDER)) {
                    if (explicitId.equals(ALL_WILDCARD)) {
                        return registry.stream().collect(Collectors.toSet());
                    }
                    return registry.entrySet().stream().filter(e -> e.getKey().location().getNamespace()
                            .equals(explicitId.getNamespace())).map(Map.Entry::getValue).collect(Collectors.toSet());
                }
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
