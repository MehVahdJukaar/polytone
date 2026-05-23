package net.mehvahdjukaar.polytone.content.slotify;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface SlotifyScreen {

    void polytone$renderExtraSprites(GuiGraphicsExtractor poseStack, int mouseX, int mouseY, float partialTicks);

    boolean polytone$hasSprites();

    ScreenModifier polytone$getModifier();
}
