package net.mehvahdjukaar.polytone.bedrock.model;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.bedrock.molang.MolangExpr;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

// The direction every emitter shape carries: either the keyword inwards/outwards (relative to the shape's own
// surface) or an explicit vector.
public record BedrockShapeDirection(Mode mode, @Nullable MolangExpr.Vec3 custom) {

    public static final BedrockShapeDirection OUTWARDS = new BedrockShapeDirection(Mode.OUTWARDS, null);

    public static final Codec<BedrockShapeDirection> CODEC =
            Codec.either(Codec.STRING, MolangExpr.Vec3.CODEC).comapFlatMap(
                    either -> either.map(BedrockShapeDirection::fromKeyword,
                            vec -> DataResult.success(new BedrockShapeDirection(Mode.CUSTOM, vec))),
                    dir -> dir.mode == Mode.CUSTOM
                            ? Either.right(dir.custom)
                            : Either.left(dir.mode.name().toLowerCase(Locale.ROOT)));

    private static DataResult<BedrockShapeDirection> fromKeyword(String keyword) {
        return switch (keyword.toLowerCase(Locale.ROOT)) {
            case "outwards" -> DataResult.success(OUTWARDS);
            case "inwards" -> DataResult.success(new BedrockShapeDirection(Mode.INWARDS, null));
            default -> DataResult.error(() -> "Unknown shape direction '" + keyword + "'");
        };
    }

    public enum Mode {
        OUTWARDS,
        INWARDS,
        CUSTOM
    }
}
