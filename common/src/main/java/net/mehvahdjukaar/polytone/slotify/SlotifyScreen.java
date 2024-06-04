package net.mehvahdjukaar.polytone.slotify;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

public interface SlotifyScreen {

    void polytone$renderExtraSprites(GuiGraphics poseStack);

    boolean polytone$hasSprites();

    ScreenModifier polytone$getModifier();
}
