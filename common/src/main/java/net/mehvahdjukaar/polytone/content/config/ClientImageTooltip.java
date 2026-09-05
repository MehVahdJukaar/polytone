package net.mehvahdjukaar.polytone.content.config;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class ClientImageTooltip implements ClientTooltipComponent {
    private static final int SIDE_MARGIN = 4;
    private static final int TOP_MARGIN = 4;
    private static final int BOTTOM_MARGIN = 4;

    private final Identifier texture;
    private final int width;
    private final int height;

    public ClientImageTooltip(Identifier texture, int width, int height) {
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth(Font font) {
        return width + SIDE_MARGIN * 2;
    }

    @Override
    public int getHeight(Font font) {
        return height + TOP_MARGIN + BOTTOM_MARGIN;
    }

    @Override
    public void extractImage(Font font, int x, int y, int tooltipWidth, int tooltipHeight, GuiGraphicsExtractor guiGraphics) {
        int imageX = x + (tooltipWidth - width) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, imageX, y + TOP_MARGIN,
                0.0F, 0.0F, width, height, width, height);
    }
}
