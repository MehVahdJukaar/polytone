package net.mehvahdjukaar.polytone.content.config;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class BoolConfig extends PolyConfig<Boolean> implements OptionInstance.CycleableValueSet<Boolean> {

    public static final SchemaCodec<BoolConfig> CODEC = PolyConfig.validated(
            SchemaRecord.create(BoolConfig.class, i -> PolyConfig.commonFields(i, Codec.BOOL)
                    .apply(i, BoolConfig::new)));

    private static final List<Boolean> VALUES = ImmutableList.of(Boolean.TRUE, Boolean.FALSE);

    protected BoolConfig(Optional<String> valueTranslation, Map<String, Boolean> presets,
                         Map<String, Boolean> sectionPresets, int order,
                         Optional<String> section, Optional<Integer> sectionOrder,
                         Optional<PerformanceImpact> performanceImpact,
                         boolean wide, Map<String, TooltipImage> tooltipImages, boolean defaultValue) {
        super(valueTranslation, presets, sectionPresets, order, section, sectionOrder,
                performanceImpact, wide, tooltipImages, defaultValue);
    }

    public BoolConfig(Optional<String> valueTranslation, boolean defaultValue) {
        this(valueTranslation, Map.of(), Map.of(), 0, Optional.empty(), Optional.empty(),
                Optional.empty(), false, Map.of(), defaultValue);
    }

    @Override
    public Optional<Boolean> validateValue(Boolean object) {
        return Optional.of(object);
    }

    @Override
    public Codec<Boolean> codec() {
        return Codec.BOOL;
    }

    @Override
    public MutableComponent formatValue(Boolean value) {
        // Same constants vanilla uses everywhere else for boolean toggles (Music: ON / Music: OFF).
        return (value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF).copy();
    }

    @Override
    public CycleButton.ValueListSupplier<Boolean> valueListSupplier() {
        return CycleButton.ValueListSupplier.create(VALUES);
    }

    @Override
    public Function<OptionInstance<Boolean>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<Boolean> tooltipSupplier, Options options, int i, int j, int k, Consumer<Boolean> consumer) {
        return (optionInstance) -> {
            Objects.requireNonNull(optionInstance);
            return CycleButton.builder(optionInstance.toString)
                    .withInitialValue(optionInstance.get())
                    .withValues(this.valueListSupplier())
                    .withTooltip(tooltipSupplier)
                    .displayOnlyValue()
                    .create(i, j, k, 20, Component.empty(), (cycleButton, object) -> {
                        this.valueSetter().set(optionInstance, object);
                        consumer.accept(object);
                    });
        };
    }
}
