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

/**
 * A rounded chat-bubble widget: white interior, black outline, fixed height, any width.
 * <p>
 * The body sprite is a horizontal nine-slice (left cap + stretchable center + right cap, see
 * {@code chat_bubble_body.png.mcmeta}), so the bubble grows horizontally as its text gets longer
 * while keeping its rounded caps pixel-perfect.
 * <p>
 * It also carries its little downward-facing "tail" (drawn as a plain sprite, not a separate
 * widget). Use {@link #renderPointingAt} to place the whole thing above a target like a tooltip
 * that never clips off-screen, with the tail locked onto the target's center.
 */
public class ChatBubbleWidget extends AbstractWidget {

    private static final Identifier BODY = Polytone.res("widget/chat_bubble_body");
    private static final Identifier TAIL = Polytone.res("widget/chat_bubble_tail");

    /** Fixed native height of the bubble sprite. Render at this height for the horizontal-only slice path. */
    public static final int HEIGHT = 12;
    private static final int TAIL_WIDTH = 7;
    private static final int TAIL_HEIGHT = 4;
    /** Apex (downward point) column inside the tail sprite. */
    private static final int TAIL_TIP = 3;

    /** Horizontal gap between the rounded caps and the text. Keeps text clear of the corner curve. */
    private static final int PADDING = 6;
    /** Keep the tail this far in from each cap, so it stays over the bubble's flat (non-rounded) span. */
    private static final int CAP_INSET = 3;
    /** Keep the whole assembly this far from the screen edges. */
    private static final int SCREEN_MARGIN = 3;
    /** Vertical gap between the tail's tip and the pointed-at target. */
    private static final int TIP_GAP = 1;

    /** Period of one full up-down bob, in milliseconds. */
    private static final long BOB_PERIOD_MS = 2200L;

    private final Font font;
    private int textColor = 0xFF000000; // black, since the interior is white
    private boolean animated = false;

    public ChatBubbleWidget(int x, int y, Component message) {
        super(x, y, measureWidth(message), HEIGHT, message);
        this.font = Minecraft.getInstance().font;
    }

    private static int measureWidth(Component message) {
        return Minecraft.getInstance().font.width(message) + PADDING * 2;
    }

    /** Update the displayed text and grow/shrink the bubble to fit it. */
    public void setText(Component message) {
        this.setMessage(message);
        this.setWidth(measureWidth(message));
    }

    public void setTextColor(int argb) {
        this.textColor = argb;
    }

    /** When enabled, the bubble slowly bobs up 1px and back. */
    public ChatBubbleWidget setAnimated(boolean animated) {
        this.animated = animated;
        return this;
    }

    /** Current vertical bob offset: 0 or -1 (up), oscillating slowly when animated. */
    private int bobOffset() {
        if (!animated) return 0;
        double phase = (System.currentTimeMillis() % BOB_PERIOD_MS) / (double) BOB_PERIOD_MS;
        return -(int) Math.round((1 - Math.cos(phase * 2 * Math.PI)) / 2); // smooth 0 -> -1 -> 0
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Drawn at native HEIGHT -> the nine-slice renderer uses its horizontal-only branch:
        // [left cap][stretched center][right cap].
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BODY, this.getX(), this.getY(), this.getWidth(), this.getHeight());

        int textX = this.getX() + PADDING;
        int textY = this.getY() + (this.getHeight() - this.font.lineHeight) / 2 + 1;
        graphics.text(this.font, this.getMessage(), textX, textY, this.textColor, false);
    }

    /**
     * Place this bubble above {@code target}, tail pointing down at its center, and draw it — like a
     * tooltip, but it never clips off-screen. The bubble is clamped to stay on screen while the tail
     * stays locked onto the target (kept within the bubble's flat span) to preserve the illusion.
     *
     * @param screenWidth width of the screen, used to clamp the bubble on-screen
     */
    public void renderPointingAt(GuiGraphicsExtractor graphics, AbstractWidget target, int screenWidth,
                                 int mouseX, int mouseY, float partialTick) {
        int bubbleW = this.getWidth();
        int targetCenterX = target.getX() + target.getWidth() / 2;

        // vertical stack (top -> bottom): bubble, tail (overlapping its bottom outline), gap, target
        int bob = bobOffset(); // bubble + tail move together so the illusion holds
        int tailY = target.getY() - TIP_GAP - TAIL_HEIGHT + 1 + bob; // tip just above the target
        int bubbleY = tailY - HEIGHT + 1;                            // bubble bottom overlaps tail top by 1px

        // center the bubble over the target, then clamp it to stay fully on screen (tooltip-style)
        int bubbleX = targetCenterX - bubbleW / 2;
        int maxX = screenWidth - bubbleW - SCREEN_MARGIN;
        bubbleX = maxX < SCREEN_MARGIN ? SCREEN_MARGIN
                : Math.max(SCREEN_MARGIN, Math.min(bubbleX, maxX));

        // point the tail's tip at the target, but keep the tail within the bubble's flat span
        int tailX = targetCenterX - TAIL_TIP;
        int tailMin = bubbleX + CAP_INSET;
        int tailMax = bubbleX + bubbleW - TAIL_WIDTH - CAP_INSET;
        tailX = tailMax < tailMin ? tailMin : Math.max(tailMin, Math.min(tailX, tailMax));

        this.setX(bubbleX);
        this.setY(bubbleY);
        this.extractRenderState(graphics, mouseX, mouseY, partialTick); // body + text

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TAIL, tailX, tailY, TAIL_WIDTH, TAIL_HEIGHT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}