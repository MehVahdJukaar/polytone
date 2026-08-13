package net.mehvahdjukaar.polytone.bedrock.molang;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.StrUtils;
import net.minecraft.core.Direction.Axis;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

// One value out of a Bedrock particle file. Bedrock lets any numeric field be written as either a literal or a
// Molang expression string, so both land here and the source is kept verbatim until a MolangTranslator gets to
// it.
public record MolangExpr(String source, @Nullable Double constant) {

    public static final MolangExpr ZERO = of(0);
    public static final MolangExpr ONE = of(1);

    public static final Codec<MolangExpr> CODEC = Codec.either(Codec.DOUBLE, Codec.STRING).xmap(
            either -> either.map(MolangExpr::of, MolangExpr::of),
            expr -> expr.isConstant() ? Either.left(expr.constant) : Either.right(expr.source));

    public static MolangExpr of(double value) {
        return new MolangExpr(StrUtils.compactNumber(value), value);
    }

    public static MolangExpr of(String source) {
        String trimmed = source.trim();
        Double folded;
        try {
            folded = Double.valueOf(trimmed);
        } catch (NumberFormatException e) {
            folded = null;
        }
        return new MolangExpr(trimmed, folded);
    }

    public boolean isConstant() {
        return constant != null;
    }

    public boolean isConstant(double value) {
        return constant != null && constant == value;
    }

    public double constantOr(double fallback) {
        return constant != null ? constant : fallback;
    }

    private static <T> Codec<T> fixedSizeList(int size, Function<List<MolangExpr>, T> factory,
                                              Function<T, List<MolangExpr>> getter) {
        return CODEC.listOf().comapFlatMap(
                list -> list.size() == size
                        ? DataResult.success(factory.apply(list))
                        : DataResult.error(() -> "Expected " + size + " values, got " + list.size()),
                getter);
    }

    public record Vec2(MolangExpr x, MolangExpr y) {
        public static final Codec<Vec2> CODEC =
                fixedSizeList(2, l -> new Vec2(l.getFirst(), l.get(1)), v -> List.of(v.x, v.y));

        public static Vec2 of(double x, double y) {
            return new Vec2(MolangExpr.of(x), MolangExpr.of(y));
        }
    }

    public record Vec3(MolangExpr x, MolangExpr y, MolangExpr z) {
        public static final Vec3 ZERO = of(0, 0, 0);

        public static final Codec<Vec3> CODEC =
                fixedSizeList(3, l -> new Vec3(l.getFirst(), l.get(1), l.get(2)), v -> List.of(v.x, v.y, v.z));

        public static Vec3 of(double x, double y, double z) {
            return new Vec3(MolangExpr.of(x), MolangExpr.of(y), MolangExpr.of(z));
        }

        public MolangExpr get(Axis axis) {
            return switch (axis) {
                case X -> x;
                case Y -> y;
                case Z -> z;
            };
        }

        public boolean isZero() {
            return x.isConstant(0) && y.isConstant(0) && z.isConstant(0);
        }
    }
}
