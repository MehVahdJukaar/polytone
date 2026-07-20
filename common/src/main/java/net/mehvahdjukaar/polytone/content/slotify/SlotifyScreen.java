package net.mehvahdjukaar.polytone.content.slotify;

import net.minecraft.client.gui.GuiGraphics;

public interface SlotifyScreen {

    void polytone$renderExtraSprites(GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks);

    boolean polytone$hasSprites();

    ScreenModifier polytone$getModifier();

    // Re-run the screen's widget layout (idempotent). Used by the live editor preview to re-apply a
    // freshly edited modifier onto an already-built screen with no resource reload.
    void polytone$rebuild();

    // Re-fetch this screen's modifier from the manager (picks up a pushed preview override).
    void polytone$refreshModifier();
}
