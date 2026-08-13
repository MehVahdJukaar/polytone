package net.mehvahdjukaar.polytone.content.common.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

// Renders a ChatBubbleWidget above a target widget each frame, without participating in layout or input.
public class PointingChatBubbleOverlay implements Renderable {

    private final AbstractWidget target;
    private final IntSupplier screenWidth;
    private final Supplier<Component> messageSupplier;
    private final ChatBubbleWidget bubble;

    public PointingChatBubbleOverlay(AbstractWidget target, IntSupplier screenWidth,
                                     Supplier<Component> messageSupplier) {
        this.target = target;
        this.screenWidth = screenWidth;
        this.messageSupplier = messageSupplier;
        this.bubble = new ChatBubbleWidget(0, 0, Component.empty()).setAnimated(true);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.target.visible) return;

        Component message = this.messageSupplier.get();
        if (message == null) return;

        if (!message.equals(this.bubble.getMessage())) {
            this.bubble.setText(message);
        }
        this.bubble.renderPointingAt(graphics, this.target, this.screenWidth.getAsInt(), mouseX, mouseY, partialTick);
    }
}
