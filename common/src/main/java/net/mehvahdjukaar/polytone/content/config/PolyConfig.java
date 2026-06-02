package net.mehvahdjukaar.polytone.content.config;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * 1.21.1 stub of the 1.21.11 config system.
 *
 * <p>Pack JSONs under {@code polytone/config_entries/*.json} are parsed so packs that reference
 * configs in their other JSONs (e.g. via expressions) can still load. Each config exposes only
 * its {@code default_value} — there is no UI on this branch, so user-overridden values aren't
 * possible. {@code OptionHolder.get()} always returns the default.
 */
public abstract class PolyConfig<T> {

    public static final Codec<PolyConfig<?>> CODEC = Codec.lazyInitialized(() ->
            CodecUtils.alternatives(StringConfig.CODEC, NumberConfig.CODEC, BoolConfig.CODEC));

    private final Optional<String> valueTranslationKey;
    private final Map<String, T> presets;
    private final int displayOrder;
    private final T defaultValue;

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

    public abstract Optional<T> validateValue(T value);

    static <A, T extends PolyConfig<A>> @NotNull DataResult<T> validatePresets(T o) {
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
