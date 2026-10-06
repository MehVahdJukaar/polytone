package net.mehvahdjukaar.polytone.content.slotify;

import net.mehvahdjukaar.polytone.compat.nautilus.NautilusGuiModifierOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public interface SlotifyScreen {

    void polytone$renderExtraSprites(GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks);

    boolean polytone$hasSprites();

    ScreenModifier polytone$getModifier();

    void polytone$refreshModifier();

    void polytone$rebuild();

    static void renderExtras(GuiGraphics graphics, SlotifyScreen ss, int screenWidth, int screenHeight,
                             int mouseX, int mouseY, float partialTick) {
        if (GuiModifierPreview.isPickingEnabled() && ss instanceof Screen screen) {
            NautilusGuiModifierOverlay.render(graphics, screen, mouseX, mouseY);
        }
        var pose = graphics.pose();
        pose.pushPose();
        pose.setIdentity();
        pose.translate(screenWidth / 2F, screenHeight / 2F, 500);
        ss.polytone$renderExtraSprites(graphics, mouseX, mouseY, partialTick);
        pose.popPose();
    }
}
