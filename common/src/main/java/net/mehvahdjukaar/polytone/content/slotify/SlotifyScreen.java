package net.mehvahdjukaar.polytone.content.slotify;

import net.minecraft.client.gui.GuiGraphics;

public interface SlotifyScreen {

    void polytone$renderExtraSprites(GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks);

    boolean polytone$hasSprites();

    ScreenModifier polytone$getModifier();

    // Re-resolves and re-caches the screen's modifier (picks up a live preview override). Used by the
    // editor preview to refresh per-frame sprites/texts without a resource reload.
    void polytone$refreshModifier();

    // Re-runs the screen's init (rebuildWidgets) so geometry and widget modifiers re-bake idempotently.
    void polytone$rebuild();
}
