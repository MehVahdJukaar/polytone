package net.mehvahdjukaar.polytone.common.struc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

//a missing axis counts as 0
public record Vec3T<E>(@Nullable E x, @Nullable E y, @Nullable E z) {

    public static <E> Codec<Vec3T<E>> codec(Codec<E> expression) {
        Codec<List<E>> xyz = expression.listOf(3, 3);
        Codec<Vec3T<E>> asList = xyz.flatXmap(
                l -> DataResult.success(new Vec3T<>(l.get(0), l.get(1), l.get(2))),
                v -> v.x == null || v.y == null || v.z == null
                        ? DataResult.error(() -> "Not all axis are set")
                        : DataResult.success(List.of(v.x, v.y, v.z)));
        Codec<Vec3T<E>> perAxis = RecordCodecBuilder.create(i -> i.group(
                expression.optionalFieldOf("x").forGetter(v -> Optional.ofNullable(v.x)),
                expression.optionalFieldOf("y").forGetter(v -> Optional.ofNullable(v.y)),
                expression.optionalFieldOf("z").forGetter(v -> Optional.ofNullable(v.z))
        ).apply(i, (x, y, z) -> new Vec3T<>(x.orElse(null), y.orElse(null), z.orElse(null))));

        return SchemaCodecs.labeled(SchemaCodecs.alternatives(asList, perAxis),
                SchemaCodecs.alt("xyz", xyz),
                SchemaCodecs.alt("per axis", perAxis));
    }

    public Vector3f evaluate(ToDoubleFunction<E> evaluator) {
        return new Vector3f(
                x == null ? 0 : (float) evaluator.applyAsDouble(x),
                y == null ? 0 : (float) evaluator.applyAsDouble(y),
                z == null ? 0 : (float) evaluator.applyAsDouble(z));
    }
}
