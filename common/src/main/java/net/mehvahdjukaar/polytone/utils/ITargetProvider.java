package net.mehvahdjukaar.polytone.utils;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public interface ITargetProvider {

    Optional<Set<ResourceLocation>> explicitTargets();

    default <T> Set<T> getTargets(ResourceLocation fileId, Registry<T> registry) {
        Set<T> set = new HashSet<>();
        var explTargets = this.explicitTargets();
        Optional<T> implicitTarget = registry.getOptional(fileId);
        if (explTargets.isPresent()) {
            if (implicitTarget.isPresent() && !explTargets.get().contains(fileId)) {
                Polytone.LOGGER.error("Found Polytone file with explicit Targets ({}) also having a valid IMPLICIT (file path) Target ({})." +
                        "Consider moving it under your OWN namespace to avoid overriding other packs modifiers with the same path", explTargets.get(), fileId);
            }
            for (var explicitId : explTargets.get()) {
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
