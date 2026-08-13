package net.mehvahdjukaar.polytone.content.config;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

// 1.21.1's renderImage has no tooltip width parameter, so the image is left aligned, not centered
public class ClientImageTooltip implements ClientTooltipComponent {
    // uniform 4px breathing room on all sides, matching vanilla's bundle element spacing
    private static final int SIDE_MARGIN = 4;
    private static final int TOP_MARGIN = 4;
    private static final int BOTTOM_MARGIN = 4;

    private final ResourceLocation texture;
    private final int width;
    private final int height;

    public ClientImageTooltip(ResourceLocation texture, int width, int height) {
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth(Font font) {
        return width + SIDE_MARGIN * 2;
    }

    @Override
    public int getHeight() {
        return height + TOP_MARGIN + BOTTOM_MARGIN;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.blit(texture, x + SIDE_MARGIN, y + TOP_MARGIN, 0.0F, 0.0F, width, height, width, height);
    }
}
