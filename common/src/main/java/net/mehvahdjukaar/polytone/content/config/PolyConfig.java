package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.client.OptionInstance;
import org.jetbrains.annotations.Nullable;

public interface PolyConfig<T> extends OptionInstance.ValueSet<T> {

    Codec<PolyConfig<?>> CODEC = Codec.lazyInitialized(() ->
            CodecUtils.alternatives(StringConfig.CODEC, NumberConfig.CODEC, BoolConfig.CODEC));

    T getDefaultValue();

    @Nullable
    String getValueTranslationKey();
}
