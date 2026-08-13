package net.mehvahdjukaar.polytone.common.attributes;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMaps;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

// Everything about attribute values that Polytone computes at runtime instead of storing statically: the codec
// that folds a Colormap or an Expression into any color/float attribute, the biome context such a value is
// evaluated for, and the spatial blending that makes it fade like a normal biome attribute would.
public class DynamicAttributes {

    // set while the installed attribute system has at least one dimension level dynamic layer.
    // when false none of the blending machinery runs, not even the per sample bookkeeping
    public static boolean hasDynamicLayers = false;

    private static @Nullable Biome contextBiome;

    // Biome a dynamic value is being evaluated for. Defaults to the camera one, as it always used to.
    public static Biome biome() {
        return contextBiome != null ? contextBiome : ClientFrameTicker.getCameraBiome().value();
    }

    // Pins a supplier to one biome. Used for entries that live in a biome's own attribute map: each targeted
    // biome gets its own bound copy, so vanilla's interpolator sees a different value per biome and lerps
    // them.
    public static <T> Supplier<T> boundTo(Biome owner, Supplier<T> supplier) {
        return () -> {
            Biome previous = contextBiome;
            contextBiome = owner;
            try {
                return supplier.get();
            } finally {
                contextBiome = previous;
            }
        };
    }

    // Evaluates a dimension level entry once per biome of the interpolation kernel and folds the results.
    // Inside a biome the kernel holds a single one and this costs exactly one evaluation, like before. Only
    // within ~8 blocks of a border does it become 2 to 4.
    public static <Value> Value applyBlended(EnvironmentAttribute<Value> attribute,
                                             EnvironmentAttributeMap.Entry<Value, ?> entry,
                                             Value oldValue,
                                             @Nullable SpatialAttributeInterpolator interpolator) {
        Reference2DoubleMap<Holder<Biome>> weights = interpolator == null ? null :
                ((IExtendedInterpolator) interpolator).polytone$getBiomeWeights();

        if (weights == null || weights.isEmpty()) {
            return entry.applyModifier(oldValue);
        }
        if (weights.size() == 1) {
            return applyForBiome(weights.keySet().iterator().next().value(), entry, oldValue);
        }

        LerpFunction<Value> lerp = attribute.type().spatialLerp();
        Value result = null;
        double totalWeight = 0;
        //running weighted mean, same as SpatialAttributeInterpolator does for biome maps
        for (var e : Reference2DoubleMaps.fastIterable(weights)) {
            double weight = e.getDoubleValue();
            Value value = applyForBiome(e.getKey().value(), entry, oldValue);
            totalWeight += weight;
            result = result == null ? value : lerp.apply((float) (weight / totalWeight), result, value);
        }
        return result;
    }

    private static <Value> Value applyForBiome(Biome biome, EnvironmentAttributeMap.Entry<Value, ?> entry,
                                               Value oldValue) {
        Biome previous = contextBiome;
        contextBiome = biome;
        try {
            return entry.applyModifier(oldValue);
        } finally {
            contextBiome = previous;
        }
    }

    // Allows a Colormap or an Expression to be used wherever a color or a float attribute value is expected
    public static <A, Value> Codec<Either<A, Supplier<A>>> addDynamicValueCodec(Codec<A> originalCodec,
                                                                               AttributeType<Value> type) {
        if (type == AttributeTypes.ARGB_COLOR || type == AttributeTypes.RGB_COLOR) {
            Codec<Supplier<Integer>> intCodec = Colormap.REFERENCE_OR_EXPRESSION
                    .xmap(c -> () -> {
                                ClientLevel level = Minecraft.getInstance().level;
                                if (level == null) return 0;
                                return c.sampleColor(level, null, ClientFrameTicker.getCameraPos(), biome(), null);
                            },
                            supplier -> new IColorGetter.StaticColor(supplier.get()));

            return Codec.either(originalCodec, (Codec) intCodec);
        } else if (type == AttributeTypes.FLOAT || type == AttributeTypes.ANGLE_DEGREES) {
            Codec<Supplier<Float>> floatCodec = IBlockExp.CODEC
                    .xmap(e -> () -> {
                                ClientLevel level = Minecraft.getInstance().level;
                                if (level == null) return 0f;
                                return (float) e.evaluate(level, ClientFrameTicker.getCameraPos(), null);
                            },
                            ex -> IBlockExp.ZERO);
            return Codec.either(originalCodec, (Codec) floatCodec);
        }
        return SchemaCodecs.eitherLeft(originalCodec);
    }
}
