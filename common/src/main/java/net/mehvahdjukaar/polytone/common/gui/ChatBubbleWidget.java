package net.mehvahdjukaar.polytone.common.gui;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;

public class ChatBubbleWidget extends AbstractWidget {

    private static final Identifier BODY = Polytone.res("widget/chat_bubble_body");
    private static final Identifier TAIL = Polytone.res("widget/chat_bubble_tail");

    public static final int HEIGHT = 12;
    private static final int TAIL_WIDTH = 7;
    private static final int TAIL_HEIGHT = 4;

    private static final int PADDING = 6;
    private static final int CAP_INSET = 3;
    private static final int SCREEN_MARGIN = 3;
    private static final long BOB_PERIOD_MS = 2200L;

    private final Font font;
    private int textColor = CommonColors.BLACK;
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

    public ChatBubbleWidget setTextColor(int textColor) {
        this.textColor = textColor;
        return this;
    }

    private int bobOffset() {
        if (!animated) return 0;
        double phase = (System.currentTimeMillis() % BOB_PERIOD_MS) / (double) BOB_PERIOD_MS;
        return -(int) Math.round((1 - Math.cos(phase * 2 * Math.PI)) / 2);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BODY, getX(), getY(), getWidth(), getHeight());

        int textX = getX() + PADDING;
        int textY = getY() + (getHeight() - font.lineHeight) / 2 + 1;
        graphics.text(font, getMessage(), textX, textY, textColor, false);
    }

    public void renderPointingAt(GuiGraphicsExtractor graphics, AbstractWidget target, int screenWidth,
                                 int mouseX, int mouseY, float partialTick) {
        int bubbleW = this.getWidth();
        int centerX = target.getX() + target.getWidth() / 2;
        int tailY = target.getY() - TAIL_HEIGHT + bobOffset();
        int bubbleY = tailY - HEIGHT + 1;

        // clamp throws when min > max
        int maxX = screenWidth - bubbleW - SCREEN_MARGIN;
        int bubbleX = maxX < SCREEN_MARGIN ? SCREEN_MARGIN : Math.clamp(centerX - bubbleW / 2, SCREEN_MARGIN, maxX);

        int tailMin = bubbleX + CAP_INSET;
        int tailMax = bubbleX + bubbleW - TAIL_WIDTH - CAP_INSET;
        int tailX = tailMax < tailMin ? tailMin : Math.clamp(centerX - 3, tailMin, tailMax);

        this.setX(bubbleX);
        this.setY(bubbleY);
        this.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TAIL, tailX, tailY, TAIL_WIDTH, TAIL_HEIGHT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
