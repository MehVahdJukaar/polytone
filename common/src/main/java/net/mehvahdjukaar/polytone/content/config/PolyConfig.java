package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.client.Options.genericValueLabel;

public interface PolyConfig<T> extends OptionInstance.ValueSet<T> {

    Codec<PolyConfig<?>> CODEC = Codec.lazyInitialized(() ->
            CodecUtils.alternatives(StringConfig.CODEC, NumberConfig.CODEC, BoolConfig.CODEC));


    T getDefaultValue();

    @Nullable
    String getValueTranslationKey();


}
