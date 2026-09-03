package net.mehvahdjukaar.polytone.content.slotify;

import net.mehvahdjukaar.polytone.compat.nautilus.NautilusGuiModifierOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public interface SlotifyScreen {

    void polytone$renderExtraSprites(GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks);

    boolean polytone$hasSprites();

    ScreenModifier polytone$getModifier();

    //re-resolves and re-caches the screen modifier, picking up a live preview override. lets the editor
    //refresh sprites and texts without a resource reload
    void polytone$refreshModifier();

    //re-runs the screen init (rebuildWidgets) so geometry and widget modifiers re-bake idempotently
    void polytone$rebuild();

    //both platform screen render hooks land here, only the event wiring differs. renderExtraSprites
    //no ops when the screen has no modifier so calling it every frame is fine
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
