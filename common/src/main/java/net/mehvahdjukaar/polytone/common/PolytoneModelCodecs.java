package net.mehvahdjukaar.polytone.common;

import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.block.model.Variant;
import org.jetbrains.annotations.Nullable;

/**
 * Editor-facing companions for the block-variant model state that {@link SimpleModelStateExtension}
 * extends. Only touched from client code (the deserializer mixin and the PackEditor compat), so its
 * reference to the client {@code SimpleModelState} type never loads on a dedicated server.
 */
public final class PolytoneModelCodecs {

    private PolytoneModelCodecs() {}

    /**
     * The live wrapped {@code SimpleModelState} map codec the blockstate chain uses, captured by
     * {@code VariantDeserializerMixin} when it wraps the vanilla codec at class init. Null until then.
     */
    public static volatile @Nullable MapCodec<Variant.SimpleModelState> WRAPPED;

    /**
     * The flat JSON a variant model state accepts once Polytone is present: the vanilla
     * {@code x}/{@code y}/{@code z} rotations (as free floats here) and {@code uvlock}, plus
     * Polytone's {@code xoffset}/{@code yoffset}/{@code zoffset}. Shape only - never used to decode
     * or encode real data, just to give the editor a schema for the otherwise opaque wrapper.
     */
    public static final MapCodec<Variant.SimpleModelState> EDITOR_SHAPE = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.FLOAT.optionalFieldOf("x", 0f).forGetter(a -> 0f),
                    Codec.FLOAT.optionalFieldOf("y", 0f).forGetter(a -> 0f),
                    Codec.FLOAT.optionalFieldOf("z", 0f).forGetter(a -> 0f),
                    Codec.BOOL.optionalFieldOf("uvlock", false).forGetter(Variant.SimpleModelState::uvLock),
                    Codec.FLOAT.optionalFieldOf("xoffset", 0f).forGetter(a -> 0f),
                    Codec.FLOAT.optionalFieldOf("yoffset", 0f).forGetter(a -> 0f),
                    Codec.FLOAT.optionalFieldOf("zoffset", 0f).forGetter(a -> 0f)
            ).apply(instance, (x, y, z, uvlock, xOffset, yOffset, zOffset) ->
                    new Variant.SimpleModelState(Quadrant.R0, Quadrant.R0, Quadrant.R0, uvlock)));
}
