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
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

// What a colormap or expression attribute value is evaluated against (biome, value from the layers before),
// plus the codec that lets it into any color/float attribute and the per biome blending
public class DynamicAttributeContext {

    // true while the installed system has a dimension level dynamic layer, so the probe records biome weights
    public static boolean hasDynamicLayers = false;

    private static @Nullable Biome biome;
    private static @Nullable Object incomingValue;

    // defaults to the camera biome
    public static Biome biome() {
        return biome != null ? biome : ClientFrameTicker.getCameraBiome().value();
    }

    public static double incomingNumber() {
        return incomingValue instanceof Number n ? n.doubleValue() : 0;
    }

    public static <T> T inBiome(Biome owner, Supplier<T> body) {
        Biome previous = biome;
        biome = owner;
        try {
            return body.get();
        } finally {
            biome = previous;
        }
    }

    // scoped to one layer application, see EnvironmentAttributeEntryMixin
    public static <T> T withIncoming(@Nullable Object value, Supplier<T> body) {
        Object previous = incomingValue;
        incomingValue = value;
        try {
            return body.get();
        } finally {
            incomingValue = previous;
        }
    }

    // biome entries get one bound copy per targeted biome so vanilla's interpolator lerps between them
    public static <T> Supplier<T> boundTo(Biome owner, Supplier<T> supplier) {
        return () -> inBiome(owner, supplier);
    }

    // dimension level entries: evaluated once per biome in the interpolation kernel, then folded. Inside a
    // biome thats a single evaluation, near a border 2 to 4
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
            return inBiome(weights.keySet().iterator().next().value(), () -> entry.applyModifier(oldValue));
        }

        LerpFunction<Value> lerp = attribute.type().spatialLerp();
        Value result = null;
        double totalWeight = 0;
        //running weighted mean, same as SpatialAttributeInterpolator does for biome maps
        for (var e : Reference2DoubleMaps.fastIterable(weights)) {
            double weight = e.getDoubleValue();
            Value value = inBiome(e.getKey().value(), () -> entry.applyModifier(oldValue));
            totalWeight += weight;
            result = result == null ? value : lerp.apply((float) (weight / totalWeight), result, value);
        }
        return result;
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
            Codec<Supplier<Float>> floatCodec = IBlockExp.CODEC_LEGACY
                    .xmap(e -> () -> {
                                ClientLevel level = Minecraft.getInstance().level;
                                if (level == null) return 0f;
                                return (float) e.evaluate(level, ClientFrameTicker.getCameraPos(), null, incomingNumber());
                            },
                            ex -> IBlockExp.ZERO);
            return Codec.either(originalCodec, (Codec) floatCodec);
        }
        return SchemaCodecs.eitherLeft(originalCodec);
    }
}
