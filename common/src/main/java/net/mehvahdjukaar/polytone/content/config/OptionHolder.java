package net.mehvahdjukaar.polytone.content.config;

import net.minecraft.resources.ResourceLocation;

/**
 * Stub of the 1.21.11 {@code OptionHolder}. On 1.21.1 there is no config screen and no
 * disk persistence, so {@link #get()} always returns the {@link PolyConfig#getDefaultValue() default}.
 */
public class OptionHolder<T> {

    public final ResourceLocation fileId;
    public final PolyConfig<T> config;

    private OptionHolder(PolyConfig<T> config, ResourceLocation id) {
        this.config = config;
        this.fileId = id;
    }

    public T get() {
        return config.getDefaultValue();
    }

    public static <T> OptionHolder<T> create(PolyConfig<T> config, ResourceLocation id) {
        return new OptionHolder<>(config, id);
    }
}
