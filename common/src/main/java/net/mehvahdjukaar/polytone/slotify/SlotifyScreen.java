package net.mehvahdjukaar.polytone.slotify;

import net.minecraft.client.gui.GuiGraphics;

public interface SlotifyScreen {

    void polytone$renderExtraSprites(GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks);

    boolean polytone$hasSprites();

    ScreenModifier polytone$getModifier();
}
