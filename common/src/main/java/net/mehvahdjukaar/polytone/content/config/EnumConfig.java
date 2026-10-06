package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

//builtin only, for conveinenc
public class EnumConfig<E extends Enum<E> & StringRepresentable> extends PolyConfig<E> implements OptionInstance.CycleableValueSet<E> {

    private final List<E> values;
    private final Codec<E> codec;

    public EnumConfig(E defaultValue, Optional<String> section) {
        super(Optional.empty(), Map.of(), Map.of(), 0, section, Optional.empty(),
                Optional.empty(), false, Map.of(), defaultValue);
        E[] constants = defaultValue.getDeclaringClass().getEnumConstants();
        this.values = List.of(constants);
        this.codec = StringRepresentable.fromEnum(() -> constants);
    }

    @Override
    public Optional<E> validateValue(E object) {
        return Optional.of(object);
    }

    @Override
    public Codec<E> codec() {
        return codec;
    }

    @Override
    public MutableComponent formatValue(E value) {
        return Component.literal(value.getSerializedName());
    }

    @Override
    public CycleButton.ValueListSupplier<E> valueListSupplier() {
        return CycleButton.ValueListSupplier.create(this.values);
    }

    @Override
    public Function<OptionInstance<E>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<E> tooltipSupplier, Options options, int i, int j, int k, Consumer<E> consumer) {
        return (optionInstance) -> {
            Objects.requireNonNull(optionInstance);
            // withValues before withInitialValue - see BoolConfig#createButton
            return CycleButton.builder(optionInstance.toString)
                    .withValues(this.valueListSupplier())
                    .withInitialValue(optionInstance.get())
                    .withTooltip(tooltipSupplier)
                    .displayOnlyValue()
                    .create(i, j, k, 20, Component.empty(), (cycleButton, object) -> {
                        this.valueSetter().set(optionInstance, object);
                        consumer.accept(object);
                    });
        };
    }
}
