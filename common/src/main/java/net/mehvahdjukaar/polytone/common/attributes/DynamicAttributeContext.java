package net.mehvahdjukaar.polytone.common.attributes;

import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMaps;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.*;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

//nonsense stateful backing class
public class DynamicAttributeContext {

    // true while the installed system has a dimension level dynamic layer, so the probe records biome weights
    public static boolean hasDynamicLayers = false;

    private static boolean timeBlendRequested;

    private static @Nullable Biome biome;
    private static @Nullable Object previousLayerValue;

    // defaults to the camera biome
    public static Biome biome() {
        return biome != null ? biome : ClientFrameTicker.getCameraBiome().value();
    }

    public static void markTimeBlendRequested() {
        timeBlendRequested = true;
    }

    public static boolean consumeTimeBlendRequest() {
        boolean requested = timeBlendRequested;
        timeBlendRequested = false;
        return requested;
    }

    public static double previousLayerNumber() {
        return previousLayerValue instanceof Number n ? n.doubleValue() : 0;
    }

    public static int sampleColor(IColorGetter colormap) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return 0;
        return colormap.sampleColor(level, null, ClientFrameTicker.getCameraPos(), biome(), null);
    }

    public static float evaluate(IBlockExp expression) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return 0f;
        return (float) expression.evaluate(level, ClientFrameTicker.getCameraPos(), null, biome(), previousLayerNumber());
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
    public static <T> T wrapAttributeMod(@Nullable Object valueBelow, Supplier<T> body) {
        Object previous = previousLayerValue;
        previousLayerValue = valueBelow;
        try {
            return body.get();
        } finally {
            previousLayerValue = previous;
        }
    }

    public static <Value> Value applyBlended(EnvironmentAttribute<Value> attribute,
                                             EnvironmentAttributeMap.Entry<Value, ?> entry,
                                             Value oldValue,
                                             @Nullable SpatialAttributeInterpolator interpolator) {
        Reference2DoubleMap<Holder<Biome>> weights = interpolator == null ? null :
                ((IExtendedAttrInterpolator) interpolator).polytone$getBiomeWeights();

        if (weights == null || weights.isEmpty()) {
            return entry.applyModifier(oldValue);
        }
        if (weights.size() == 1) {
            return applyInBiome(weights.keySet().iterator().next(), entry, oldValue);
        }

        LerpFunction<Value> lerp = attribute.type().spatialLerp();
        Value result = null;
        double totalWeight = 0;
        //running weighted mean, same as SpatialAttributeInterpolator does for biome maps
        for (var e : Reference2DoubleMaps.fastIterable(weights)) {
            double weight = e.getDoubleValue();
            Value value = applyInBiome(e.getKey(), entry, oldValue);
            totalWeight += weight;
            result = result == null ? value : lerp.apply((float) (weight / totalWeight), result, value);
        }
        return result;
    }

    private static <Value> Value applyInBiome(Holder<Biome> biome, EnvironmentAttributeMap.Entry<Value, ?> entry, Value oldValue) {
        return inBiome(biome.value(), () -> entry.applyModifier(oldValue));
    }
}
