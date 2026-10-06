package net.mehvahdjukaar.polytone.content.slotify;

import net.mehvahdjukaar.polytone.compat.nautilus.NautilusGuiModifierOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public interface SlotifyScreen {

    // called after the screen has drawn everything. sprites are authored around the screen center so the
    // pose has to be reset first, whatever the screen left on it
    static void renderExtras(GuiGraphicsExtractor graphics, SlotifyScreen ss, int screenWidth, int screenHeight,
                             int mouseX, int mouseY, float partialTick) {
        if (GuiModifierPreview.isPickingEnabled() && ss instanceof Screen screen) {
            NautilusGuiModifierOverlay.render(graphics, screen, mouseX, mouseY);
        }
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.identity();
        pose.translate(screenWidth / 2F, screenHeight / 2F);
        ss.polytone$renderExtraSprites(graphics, mouseX, mouseY, partialTick);
        pose.popMatrix();
    }

    void polytone$renderExtraSprites(GuiGraphicsExtractor poseStack, int mouseX, int mouseY, float partialTicks);

    boolean polytone$hasSprites();

    ScreenModifier polytone$getModifier();

    void polytone$rebuild();

    void polytone$refreshModifier();
}
