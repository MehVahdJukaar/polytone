package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.minecraft.client.OptionInstance;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Map;

public interface PolyConfig<T> extends OptionInstance.ValueSet<T> {

    Codec<PolyConfig<?>> CODEC = Codec.lazyInitialized(() ->
            CodecUtils.alternatives(StringConfig.CODEC, NumberConfig.CODEC, BoolConfig.CODEC));

    T getDefaultValue();

    Map<String, T> getPresets();

    @Nullable
    String getValueTranslationKey();

    static <A, T extends PolyConfig<A>> @NonNull DataResult<T> validatePresets(T o, Map<String, A> presets) {
        //validate presets
        for (var entry : presets.entrySet()) {
            if (o.validateValue(entry.getValue()).isEmpty()) {
                return DataResult.error(() -> "Preset value '" + entry.getValue() + "' for preset '" + entry.getKey() + "' is not valid");
            }
        }
        return DataResult.success(o);
    }
}
