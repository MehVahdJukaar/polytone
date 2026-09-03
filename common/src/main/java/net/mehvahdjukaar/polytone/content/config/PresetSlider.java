package net.mehvahdjukaar.polytone.content.config;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

//mirrors vanilla's graphics Preset slider
record PresetSlider(int maxIndex, IntFunction<Component> labelGetter,
                    Runnable onDragStart, IntConsumer onPicked)
        implements OptionInstance.ValueSet<Integer> {

    @Override
    public Function<OptionInstance<Integer>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<Integer> tooltipSupplier, Options options,
            int x, int y, int width, Consumer<Integer> onChanged) {
        return option -> new Widget(x, y, width, 20, option, tooltipSupplier);
    }

    @Override
    public Optional<Integer> validateValue(Integer value) {
        return (value >= 0 && value <= maxIndex) ? Optional.of(value) : Optional.empty();
    }

    @Override
    public Codec<Integer> codec() {
        return Codec.intRange(0, maxIndex);
    }

    private double sliderPos(int index) {
        return maxIndex == 0 ? 0 : index / (double) maxIndex;
    }

    private final class Widget extends AbstractSliderButton {
        private final OptionInstance<Integer> option;
        private final OptionInstance.TooltipSupplier<Integer> tooltipSupplier;
        private boolean smoothDragging;

        Widget(int x, int y, int width, int height, OptionInstance<Integer> option,
               OptionInstance.TooltipSupplier<Integer> tooltipSupplier) {
            super(x, y, width, height, labelGetter.apply(option.get()), sliderPos(option.get()));
            this.option = option;
            this.tooltipSupplier = tooltipSupplier;
            this.updateMessage();
        }

        private int nearestIndex() {
            return (int) Math.round(this.value * maxIndex);
        }

        @Override
        protected void updateMessage() {
            int index = nearestIndex();
            this.setMessage(labelGetter.apply(index));
            this.setTooltip(tooltipSupplier.apply(index));
        }

        @Override
        protected void applyValue() {
            //no live apply, presets are committed on release / keyboard commit only
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.smoothDragging = true;
            onDragStart.run();
            super.onClick(mouseX, mouseY);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            this.smoothDragging = true;
            super.onDrag(mouseX, mouseY, dragX, dragY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            if (this.smoothDragging) {
                this.smoothDragging = false;
                commit();
            }
            super.onRelease(mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            int dir = keyCode == InputConstants.KEY_LEFT ? -1 : keyCode == InputConstants.KEY_RIGHT ? 1 : 0;
            if (dir == 0) return super.keyPressed(keyCode, scanCode, modifiers);
            this.value = sliderPos(Mth.clamp(nearestIndex() + dir, 0, maxIndex));
            commit();
            return true;
        }

        private void commit() {
            int index = nearestIndex();
            this.value = sliderPos(index);
            if (!Objects.equals(option.get(), index)) {
                option.set(index);
                onPicked.accept(index);
            }
            this.updateMessage();
        }
    }
}
