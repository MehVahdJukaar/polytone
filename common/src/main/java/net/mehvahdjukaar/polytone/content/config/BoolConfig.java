package net.mehvahdjukaar.polytone.content.config;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BoolConfig implements OptionInstance.CycleableValueSet<Boolean>, PolyConfig<Boolean> {

    public static final Codec<BoolConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("value_translation").forGetter(c -> Optional.ofNullable(c.valueTranslationKey)),
            Codec.BOOL.fieldOf("default_value").forGetter(c -> c.defaultValue)
    ).apply(instance, BoolConfig::new));

    private static final List<Boolean> VALUES = ImmutableList.of(Boolean.TRUE, Boolean.FALSE);
    private final @Nullable String valueTranslationKey;
    private final boolean defaultValue;

    protected BoolConfig(Optional<String> valueTranslation, boolean defaultValue) {
        this.defaultValue = defaultValue;
        this.valueTranslationKey = valueTranslation.orElse(null);
    }

    @Override
    public @Nullable String getValueTranslationKey() {
        return valueTranslationKey;
    }

    @Override
    public Boolean getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Optional<Boolean> validateValue(Boolean object) {
        return Optional.of(object);
    }

    @Override
    public Codec<Boolean> codec() {
        return Codec.BOOL;
    }

    public CycleButton.ValueListSupplier<Boolean> valueListSupplier() {
        return CycleButton.ValueListSupplier.create(VALUES);
    }

    @Override
    public Function<OptionInstance<Boolean>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<Boolean> tooltipSupplier, Options options, int i, int j, int k, Consumer<Boolean> consumer) {
        return (optionInstance) -> {
            Objects.requireNonNull(optionInstance);
            return CycleButton.builder(optionInstance.toString, (Supplier<Boolean>) optionInstance::get)
                    .withValues(this.valueListSupplier())
                    .withTooltip(tooltipSupplier)
                    .displayState(CycleButton.DisplayState.VALUE)
                    .create(i, j, k, 20, Component.empty(), (cycleButton, object) -> {
                        this.valueSetter().set(optionInstance, object);
                        consumer.accept(object);
                    });
        };
    }
}
