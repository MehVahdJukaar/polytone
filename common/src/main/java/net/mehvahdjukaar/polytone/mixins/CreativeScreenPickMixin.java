package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.compat.nautilus.NautilusCreativeTabOverlay;
import net.mehvahdjukaar.polytone.content.tabs.CreativeTabPreview;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// both hooks are inert unless the editor turned picking on
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenPickMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void polytone$renderPickOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (CreativeTabPreview.isPickingEnabled()) {
            NautilusCreativeTabOverlay.render(graphics, self(), mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void polytone$pickItem(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!CreativeTabPreview.isPickingEnabled()) return;
        ItemStack picked = NautilusCreativeTabOverlay.pickAt(self(), event.x(), event.y());
        if (picked != null) {
            CreativeTabPreview.onPick(picked.copy());
            cir.setReturnValue(true);
        }
    }

    private AbstractContainerScreen<?> self() {
        return (AbstractContainerScreen<?>) (Object) this;
    }
}
