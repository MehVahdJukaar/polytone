package net.mehvahdjukaar.polytone.content.common.gui;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A rounded chat-bubble widget: white interior, black outline, fixed height, any width.
 */
public class ChatBubbleWidget extends AbstractWidget {

    private static final ResourceLocation BODY = Polytone.res("widget/chat_bubble_body");
    private static final ResourceLocation TAIL = Polytone.res("widget/chat_bubble_tail");

    public static final int HEIGHT = 12;
    private static final int TAIL_WIDTH = 7;
    private static final int TAIL_HEIGHT = 4;
    private static final int TAIL_TIP = 3;
    private static final int PADDING = 6;
    private static final int CAP_INSET = 3;
    private static final int SCREEN_MARGIN = 3;
    private static final int TIP_GAP = 1;
    private static final long BOB_PERIOD_MS = 2200L;

    private final Font font;
    private int textColor = 0xFF000000;
    private boolean animated = false;

    public ChatBubbleWidget(int x, int y, Component message) {
        super(x, y, measureWidth(message), HEIGHT, message);
        this.font = Minecraft.getInstance().font;
    }

    private static int measureWidth(Component message) {
        return Minecraft.getInstance().font.width(message) + PADDING * 2;
    }

    public void setText(Component message) {
        this.setMessage(message);
        this.setWidth(measureWidth(message));
    }

    public ChatBubbleWidget setAnimated(boolean animated) {
        this.animated = animated;
        return this;
    }

    private int bobOffset() {
        if (!animated) return 0;
        double phase = (System.currentTimeMillis() % BOB_PERIOD_MS) / (double) BOB_PERIOD_MS;
        return -(int) Math.round((1 - Math.cos(phase * 2 * Math.PI)) / 2);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(BODY, this.getX(), this.getY(), this.getWidth(), this.getHeight());

        int textX = this.getX() + PADDING;
        int textY = this.getY() + (this.getHeight() - this.font.lineHeight) / 2 + 1;
        graphics.drawString(this.font, this.getMessage(), textX, textY, this.textColor, false);
    }

    public void renderPointingAt(GuiGraphics graphics, AbstractWidget target, int screenWidth,
                                 int mouseX, int mouseY, float partialTick) {
        int bubbleW = this.getWidth();
        int targetCenterX = target.getX() + target.getWidth() / 2;

        int bob = bobOffset();
        int tailY = target.getY() - TIP_GAP - TAIL_HEIGHT + 1 + bob;
        int bubbleY = tailY - HEIGHT + 1;

        int bubbleX = targetCenterX - bubbleW / 2;
        int maxX = screenWidth - bubbleW - SCREEN_MARGIN;
        bubbleX = maxX < SCREEN_MARGIN ? SCREEN_MARGIN
                : Math.max(SCREEN_MARGIN, Math.min(bubbleX, maxX));

        int tailX = targetCenterX - TAIL_TIP;
        int tailMin = bubbleX + CAP_INSET;
        int tailMax = bubbleX + bubbleW - TAIL_WIDTH - CAP_INSET;
        tailX = tailMax < tailMin ? tailMin : Math.max(tailMin, Math.min(tailX, tailMax));

        this.setX(bubbleX);
        this.setY(bubbleY);
        this.renderWidget(graphics, mouseX, mouseY, partialTick);

        graphics.blitSprite(TAIL, tailX, tailY, TAIL_WIDTH, TAIL_HEIGHT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
