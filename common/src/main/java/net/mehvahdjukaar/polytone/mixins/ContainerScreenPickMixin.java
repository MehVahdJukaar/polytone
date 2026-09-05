package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.compat.nautilus.NautilusGuiModifierOverlay;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifierPreview;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifierPreview.PickedElement;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenPickMixin {

    // While the editor's picker is on, a click on a slot identifies it (fed back to the editor) and is
    // swallowed so it never moves items. Only active when picking is enabled, so normal play is untouched.
    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
    private void polytone$pickSlot(MouseButtonEvent event, boolean isDoubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!GuiModifierPreview.isPickingEnabled()) return;
        AbstractContainerScreen<?> cs = (AbstractContainerScreen<?>) (Object) this;
        PickedElement picked = NautilusGuiModifierOverlay.pickAt(cs, event.x(), event.y());
        if (picked != null) {
            GuiModifierPreview.onPick(picked);
            cir.setReturnValue(true);
        }
    }
}
