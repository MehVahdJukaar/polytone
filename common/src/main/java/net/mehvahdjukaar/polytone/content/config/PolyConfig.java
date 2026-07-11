package net.mehvahdjukaar.polytone.content.config;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.minecraft.client.OptionInstance;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Optional;

public abstract class PolyConfig<T> implements OptionInstance.ValueSet<T> {

    private final Optional<String> valueTranslationKey;
    private final Map<String, T> presets;
    private final int displayOrder;
    private final T defaultValue;

    public static final Codec<PolyConfig<?>> CODEC = Codec.lazyInitialized(() -> SchemaCodecs.alternatives(
            "string", StringConfig.CODEC,
            "number", NumberConfig.CODEC,
            "bool", BoolConfig.CODEC));

    protected PolyConfig(Optional<String> valueTranslationKey, Map<String, T> presets, int order, T defaultValue) {
        this.valueTranslationKey = valueTranslationKey;
        this.presets = presets;
        this.defaultValue = defaultValue;
        this.displayOrder = order;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public Map<String, T> getPresets() {
        return presets;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Optional<String> getValueTranslationKey() {
        return valueTranslationKey;
    }

    static <A, T extends PolyConfig<A>> @NonNull DataResult<T> validatePresets(T o) {
        //validate presets
        for (var entry : o.getPresets().entrySet()) {
            if (o.validateValue(entry.getValue()).isEmpty()) {
                return DataResult.error(() -> "Preset value '" + entry.getValue() + "' for preset '" + entry.getKey() + "' is not valid");
            }
        }
        return DataResult.success(o);
    }

    static <T, P extends PolyConfig<T>> Products.P4<RecordCodecBuilder.Mu<P>, Optional<String>, Map<String, T>, Integer, T> commonFields(
            RecordCodecBuilder.Instance<P> instance, Codec<T> typeCodec) {
        return instance.group(
                Codec.STRING.optionalFieldOf("value_translation").forGetter(PolyConfig::getValueTranslationKey),
                Codec.unboundedMap(Codec.STRING, typeCodec).optionalFieldOf("presets", Map.of()).forGetter(PolyConfig::getPresets),
                Codec.INT.optionalFieldOf("display_order", 0).forGetter(PolyConfig::getDisplayOrder),
                typeCodec.fieldOf("default_value").forGetter(PolyConfig::getDefaultValue));
    }
}
