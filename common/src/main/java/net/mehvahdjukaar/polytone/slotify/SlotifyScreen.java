package net.mehvahdjukaar.polytone.slotify;

import com.mojang.blaze3d.vertex.PoseStack;

public interface SlotifyScreen {

    void slotify$renderExtraSprites(PoseStack poseStack);

    boolean slotify$hasSprites();

    ScreenModifier slotify$getModifier();
}
