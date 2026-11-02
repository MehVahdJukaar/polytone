package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class ColorConfig extends PolyConfig<Integer> {
    public static final SchemaCodec<ColorConfig> CODEC = PolyConfig.validated(
            SchemaRecord.create(ColorConfig.class, i -> PolyConfig.commonFields(i, ColorUtils.CODEC)
                    .and(SchemaRecord.optional("alpha", Codec.BOOL, false, c -> c.hasAlpha))
                    .apply(i, ColorConfig::new)));

    private final boolean hasAlpha;

    protected ColorConfig(Optional<String> valueTranslation, Map<String, Integer> presets,
                          Map<String, Integer> sectionPresets, int order,
                          Optional<String> section, Optional<Integer> sectionOrder,
                          Optional<PerformanceImpact> performanceImpact,
                          boolean wide, Map<String, TooltipImage> tooltipImages,
                          int defaultValue, boolean hasAlpha) {
        super(valueTranslation, presets, sectionPresets, order, section, sectionOrder,
                performanceImpact, wide, tooltipImages, hasAlpha ? defaultValue : defaultValue & 0xFFFFFF);
        this.hasAlpha = hasAlpha;
    }

    @Override
    public Codec<Integer> codec() {
        return ColorUtils.CODEC;
    }

    @Override
    public Optional<Integer> validateValue(Integer object) {
        return Optional.of(hasAlpha ? object : object & 0xFFFFFF);
    }

    @Override
    public MutableComponent formatValue(Integer value) {
        return Component.literal(ColorUtils.toHexString(value, hasAlpha));
    }

    @Override
    public Function<OptionInstance<Integer>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<Integer> tooltipSupplier, Options options, int x, int y, int width, Consumer<Integer> consumer) {
        return optionInstance -> {
            Button button = new SwatchButton(x, y, width, optionInstance, b -> {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new ColorPickerScreen(optionInstance.get(), hasAlpha, mc.screen, picked -> {
                    optionInstance.set(picked);
                    consumer.accept(picked);
                }));
            });
            button.setTooltip(tooltipSupplier.apply(optionInstance.get()));
            return button;
        };
    }

    private class SwatchButton extends Button {

        private final OptionInstance<Integer> option;

        protected SwatchButton(int x, int y, int width, OptionInstance<Integer> option, OnPress onPress) {
            super(x, y, width, 20, Component.literal(option + ":"), onPress, DEFAULT_NARRATION);
            this.option = option;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            int size = this.height - 8;
            int sx = this.getX() + this.width - size - 4;
            int sy = this.getY() + 4;
            int color = option.get();
            if (!hasAlpha) color |= 0xFF000000;

            ColorSwatchWidget.renderChecker(graphics, sx + 1, sy + 1, size - 2, size - 2);
            graphics.fill(sx + 1, sy + 1, sx + size - 1, sy + size - 1, color);
            graphics.renderOutline(sx, sy, size, size, this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF000000);
        }

        @Override
        public void renderString(GuiGraphics graphics, Font font, int color) {
            int swatchSpace = this.height - 4;
            renderScrollingString(graphics, font, this.getMessage(), this.getX() + this.width / 2,
                    this.getX() + 2, this.getY(), this.getX() + this.width - 2 - swatchSpace, this.getY() + this.height, color);
        }

    }


}
