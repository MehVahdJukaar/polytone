package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.compat.nautilus.NautilusGuiModifierOverlay;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifierPreview;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifierPreview.PickedElement;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenPickMixin {

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void polytone$pickSlot(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!GuiModifierPreview.isPickingEnabled()) return;
        AbstractContainerScreen<?> cs = (AbstractContainerScreen<?>) (Object) this;
        PickedElement picked = NautilusGuiModifierOverlay.pickAt(cs, mouseX, mouseY);
        if (picked != null) {
            GuiModifierPreview.onPick(picked);
            cir.setReturnValue(true);
        }
    }
}
