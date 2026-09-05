package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ResettableOptionWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

final class PresetSlider {

    private PresetSlider() {
    }

    record PresetSliderValueSet(int maxIndex, IntFunction<Component> labelGetter,
                                        Runnable onDragStart, IntConsumer onStopPreview)
            implements OptionInstance.ValueSet<Integer> {

        @Override
        public Function<OptionInstance<Integer>, AbstractWidget> createButton(
                OptionInstance.TooltipSupplier<Integer> tooltipSupplier, Options options,
                int x, int y, int width, OptionInstance.ValueUpdateListener<? super Integer> onChanged) {
            return optionInstance -> new PresetSliderButton(x, y, width, 20,
                    optionInstance, maxIndex, labelGetter, onDragStart, onStopPreview,
                    tooltipSupplier, onChanged);
        }

        @Override
        public Optional<Integer> validateValue(Integer value) {
            return (value >= 0 && value <= maxIndex) ? Optional.of(value) : Optional.empty();
        }

        @Override
        public Codec<Integer> codec() {
            return Codec.intRange(0, maxIndex);
        }
    }

    static final class PresetSliderButton extends AbstractSliderButton implements ResettableOptionWidget {
        private final OptionInstance<Integer> option;
        private final int maxIndex;
        private final IntFunction<Component> labelGetter;
        private final Runnable onDragStart;
        private final IntConsumer onStopPreview;
        private final OptionInstance.TooltipSupplier<Integer> tooltipSupplier;
        private final OptionInstance.ValueUpdateListener<? super Integer> onChanged;
        private boolean smoothDragging;
        private int lastPreviewed;

        PresetSliderButton(int x, int y, int width, int height, OptionInstance<Integer> option,
                           int maxIndex, IntFunction<Component> labelGetter,
                           Runnable onDragStart, IntConsumer onStopPreview,
                           OptionInstance.TooltipSupplier<Integer> tooltipSupplier,
                           OptionInstance.ValueUpdateListener<? super Integer> onChanged) {
            super(x, y, width, height, labelGetter.apply(option.get()),
                    maxIndex == 0 ? 0 : option.get() / (double) maxIndex);
            this.option = option;
            this.maxIndex = maxIndex;
            this.labelGetter = labelGetter;
            this.onDragStart = onDragStart;
            this.onStopPreview = onStopPreview;
            this.tooltipSupplier = tooltipSupplier;
            this.onChanged = onChanged;
            this.lastPreviewed = option.get();
            this.updateMessage();
        }

        private int nearestIndex() {
            return (int) Math.round(this.value * maxIndex);
        }

        private double sliderPos(int index) {
            return maxIndex == 0 ? 0 : index / (double) maxIndex;
        }

        @Override
        protected void updateMessage() {
            int index = nearestIndex();
            this.setMessage(labelGetter.apply(index));
            this.setTooltip(tooltipSupplier.apply(index));
        }

        @Override
        protected void applyValue() {
            if (smoothDragging) {
                int index = nearestIndex();
                if (index != lastPreviewed) {
                    lastPreviewed = index;
                    onStopPreview.accept(index);
                }
            } else {
                commit();
            }
        }

        private void commit() {
            int index = nearestIndex();
            this.value = sliderPos(index);
            this.lastPreviewed = index;
            if (!Objects.equals(option.get(), index)) {
                option.set(index);
                onChanged.valueChanged(index);
            }
            this.updateMessage();
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClicked) {
            this.smoothDragging = true;
            this.lastPreviewed = nearestIndex();
            this.onDragStart.run();
            super.onClick(event, doubleClicked);
        }

        @Override
        protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
            this.smoothDragging = true;
            super.onDrag(event, dragX, dragY);
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            if (this.smoothDragging) {
                this.smoothDragging = false;
                commit();
            }
            super.onRelease(event);
        }

        @Override
        public void resetValue() {
            if (smoothDragging) return;
            int index = option.get();
            this.value = sliderPos(index);
            this.lastPreviewed = index;
            this.updateMessage();
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            int dir = event.isLeft() ? -1 : event.isRight() ? 1 : 0;
            if (dir != 0) {
                this.value = sliderPos(Mth.clamp(nearestIndex() + dir, 0, maxIndex));
                commit();
                return true;
            }
            return super.keyPressed(event);
        }
    }
}
