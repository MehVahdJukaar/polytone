package net.mehvahdjukaar.polytone.slotify;

import com.mojang.blaze3d.vertex.PoseStack;

public interface SlotifyScreen {

    void polytone$renderExtraSprites(PoseStack poseStack);

    boolean polytone$hasSprites();

    ScreenModifier polytone$getModifier();
}
