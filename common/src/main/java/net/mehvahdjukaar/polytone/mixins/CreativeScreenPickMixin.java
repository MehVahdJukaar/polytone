package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.content.tabs.CreativeTabOverlay;
import net.mehvahdjukaar.polytone.content.tabs.CreativeTabPreview;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Editor item picker on the creative screen: draws the inspector overlay and turns clicks into picks.
 * Both hooks are inert unless the editor turned picking on, so normal play is untouched.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenPickMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void polytone$renderPickOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (CreativeTabPreview.isPickingEnabled()) {
            CreativeTabOverlay.render(graphics, self(), mouseX, mouseY);
        }
    }

    // A click on an item identifies it (fed back to the editor) and is swallowed so it never grabs a stack.
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void polytone$pickItem(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!CreativeTabPreview.isPickingEnabled()) return;
        ItemStack picked = CreativeTabOverlay.pickAt(self(), event.x(), event.y());
        if (picked != null) {
            CreativeTabPreview.onPick(picked.copy());
            cir.setReturnValue(true);
        }
    }

    private AbstractContainerScreen<?> self() {
        return (AbstractContainerScreen<?>) (Object) this;
    }
}
