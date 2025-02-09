package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class Parsed<T> {

    private final boolean isEnabled;
    private final T value;
    private final ResourceLocation id;

    private Parsed(boolean isEnabled, T value, ResourceLocation id) {
        this.isEnabled = isEnabled;
        this.value = value;
        this.id = id;
    }

    public static <A> Parsed<A> success(A value, ResourceLocation id) {
        return new Parsed<>(true, value, id);
    }

    public static <A> Parsed<A> of(A value, ResourceLocation id, boolean enabled) {
        return new Parsed<>(enabled, value, id);
    }

    public T getResultOrPartial() {
        return value;
    }

    public ResourceLocation getId() {
        return id;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Optional<T> asOptional() {
        if (isEnabled) {
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    @Nullable
    public T orNull() {
        return isEnabled ? value : null;
    }

    private static final Codec<Boolean> CONDITION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("polytone_ignore", false).forGetter(b -> b),
            Codec.withAlternative(Codec.STRING.listOf(), Codec.STRING, List::of)
                    .optionalFieldOf("require_mods", List.of()).forGetter(b -> List.of())
    ).apply(instance, (b, l) -> {
        if (b) {
            return false;
        }
        for (String s : l) {
            if (!PlatStuff.isModLoaded(s)) {
                return false;
            }
        }
        return true;
    }));

    public static <T, J> Parsed<T> parseAlways(Decoder<T> codec, J input, DynamicOps<J> ops,
                                               ResourceLocation id, String jsonTypeName) {
        return parseOptionalOrPartial(codec, codec, input, ops, id, jsonTypeName);
    }

    public static <T, J> Optional<T> parseOptional(Decoder<T> codec, J input, DynamicOps<J> ops,
                                                   ResourceLocation id, String jsonTypeName) {
        Boolean enabled = CONDITION_CODEC.decode(ops, input).getOrThrow().getFirst();
        try {
            if (enabled) {
                return Optional.of(codec.decode(ops, input).getOrThrow().getFirst());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new JsonParseException("Failed to decode " + jsonTypeName + " from file \"" + id + ".json\": ", e);
        }
    }

    @Nullable
    public static <T, J> T parseOrNull(Decoder<T> codec, J input, DynamicOps<J> ops,
                                       ResourceLocation id, String jsonTypeName) {
        return Parsed.parseOptional(codec, input, ops, id, jsonTypeName).orElse(null);
    }


    public static <T, J> Parsed<T> parseOptionalOrPartial(Decoder<T> fullCodec, Decoder<T> partialCodec, J input, DynamicOps<J> ops,
                                                          ResourceLocation id, String jsonTypeName) {
        Boolean enabled = CONDITION_CODEC.decode(ops, input).getOrThrow().getFirst();
        T value;
        try {
            if (enabled) {
                value = fullCodec.decode(ops, input).getOrThrow().getFirst();
            } else {
                value = partialCodec.decode(ops, input).getOrThrow().getFirst();
            }
        } catch (Exception e) {
            throw new JsonParseException("Failed to decode " + jsonTypeName + " from file \"" + id + ".json\": ", e);
        }
        return new Parsed<>(enabled, value, id);
    }

}
