package net.mehvahdjukaar.polytone.content.config;

import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

//ML class
public class ColorFieldWidget extends AbstractContainerWidget {

    private static final int GAP = 4;

    private final EditBox hexBox;
    private final ColorSwatchWidget swatch;
    private final boolean hasAlpha;
    private int color;
    private final List<AbstractWidget> children;

    public ColorFieldWidget(int width, int height, int initial, Consumer<Integer> onChange, @Nullable Consumer<Integer> onSwatchClick) {
        this(width, height, initial, true, onChange, onSwatchClick);
    }

    public ColorFieldWidget(int width, int height, int initial, boolean hasAlpha,
                            Consumer<Integer> onChange, @Nullable Consumer<Integer> onSwatchClick) {
        super(0, 0, width, height, Component.empty());
        this.hasAlpha = hasAlpha;
        this.color = sanitize(initial);

        Font font = Minecraft.getInstance().font;
        int swatchSize = height;
        this.hexBox = new EditBox(font, 0, 0, width - swatchSize - GAP, height, Component.empty());
        this.hexBox.setMaxLength(hasAlpha ? 9 : 7);
        this.hexBox.setValue(ColorUtils.toHexString(color, hasAlpha));
        this.swatch = new ColorSwatchWidget(swatchSize, height, opaqueIfNeeded(color), onSwatchClick);
        this.hexBox.setResponder(str -> {
            try {
                int c = sanitize(ColorUtils.parseHex(str));
                this.color = c;
                this.swatch.setColor(opaqueIfNeeded(c));
                this.hexBox.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
                onChange.accept(c);
            } catch (Exception e) {
                this.hexBox.setTextColor(0xFF000000 | ChatFormatting.RED.getColor());
            }
        });
        this.children = List.of(hexBox, swatch);
    }

    private int sanitize(int c) {
        return hasAlpha ? c : c & 0xFFFFFF;
    }

    private int opaqueIfNeeded(int c) {
        return hasAlpha ? c : c | 0xFF000000;
    }

    public void setColor(int c) {
        this.color = sanitize(c);
        this.hexBox.setValue(ColorUtils.toHexString(color, hasAlpha));
        this.swatch.setColor(opaqueIfNeeded(color));
    }

    public int getColor() {
        return color;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hexBox.setPosition(getX(), getY());
        this.hexBox.render(graphics, mouseX, mouseY, partialTick);
        this.swatch.setPosition(getX() + getWidth() - swatch.getWidth(), getY());
        this.swatch.render(graphics, mouseX, mouseY, partialTick);
    }

    //container ignores this one so the EditBox caret would keep blinking
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        GuiEventListener child = this.getFocused();
        if (child != null) child.setFocused(focused);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
