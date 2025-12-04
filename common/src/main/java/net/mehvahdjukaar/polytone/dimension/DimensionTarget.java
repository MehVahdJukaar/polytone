package net.mehvahdjukaar.polytone.dimension;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Collection;

public class DimensionTarget {

    public static final Codec<DimensionTarget> CODEC = Codec.either(Targets.CODEC, DimensionTemplate.CODEC)
            .xmap(DimensionTarget::new, d -> d.target);

    public static final DimensionTarget EMPTY = new DimensionTarget(Either.left(Targets.EMPTY));

    private final Either<Targets, DimensionTemplate> target;

    private DimensionTarget(Either<Targets, DimensionTemplate> target) {
        this.target = target;
    }

    public Collection<Holder<DimensionType>> getTargets(ResourceLocation fileId, RegistryAccess registryAccess) {
        var reg = registryAccess.lookupOrThrow(Registries.DIMENSION_TYPE);
        if (target.left().isPresent()) {
            var tt = target.left().get();
            return tt.compute(fileId, reg);
        } else {
            var template = target.right().get();
            return reg.listElements().filter(h -> template.matches(h.value()))
                    .map(r -> (Holder<DimensionType>) r)
                    .toList();
        }
    }

}
