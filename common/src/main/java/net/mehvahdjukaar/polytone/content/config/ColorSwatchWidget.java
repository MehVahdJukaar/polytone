package net.mehvahdjukaar.polytone.content.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

//Also copied from moonlight class
public class ColorSwatchWidget extends AbstractWidget {

    private int color;
    @Nullable
    private final Consumer<Integer> onPress;

    public ColorSwatchWidget(int width, int height, int color, @Nullable Consumer<Integer> onPress) {
        super(0, 0, width, height, Component.empty());
        this.color = color;
        this.onPress = onPress;
        this.active = onPress != null;
        if (onPress != null) {
            this.setTooltip(Tooltip.create(Component.translatable("gui.polytone.color_pick")));
        }
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderChecker(graphics, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2);
        graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, color);
        int border = onPress != null && isHovered() ? CommonColors.WHITE : CommonColors.BLACK;
        graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), border);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.onPress != null) this.onPress.accept(this.color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    //stonel from ML GuiHelper
    public static void renderChecker(GuiGraphics graphics, int x, int y, int w, int h) {
        int cell = 4;
        for (int yy = 0; yy < h; yy += cell) {
            for (int xx = 0; xx < w; xx += cell) {
                boolean light = (((xx / cell) + (yy / cell)) & 1) == 0;
                graphics.fill(x + xx, y + yy, Math.min(x + xx + cell, x + w), Math.min(y + yy + cell, y + h),
                        light ? CommonColors.LIGHTER_GRAY : CommonColors.GRAY);
            }
        }
    }
}
