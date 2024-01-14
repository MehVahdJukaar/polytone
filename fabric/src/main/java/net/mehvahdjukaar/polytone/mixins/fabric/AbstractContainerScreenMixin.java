package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.slotify.GuiModifierManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {

    @Shadow protected int titleLabelX;

    @Shadow protected int titleLabelY;

    @Shadow protected int inventoryLabelX;

    @Shadow protected int inventoryLabelY;

    protected AbstractContainerScreenMixin(Component component) {
        super(component);
    }

    @WrapWithCondition(method = "render", at = @At(
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V",
            value = "INVOKE"
    ))
    public boolean slotifyColor(GuiGraphics graphics, int x, int y, int blitOffset,
                                @Local Slot slot){
        return GuiModifierManager.maybeChangeColor((AbstractContainerScreen<?>) (Object)this, slot, graphics, x, y, blitOffset);
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void modifyLabels(CallbackInfo ci){
        var m = GuiModifierManager.getGuiModifier((AbstractContainerScreen<?>) (Object)this);
        if(m != null){
            this.titleLabelX += m.titleX();
            this.titleLabelY += m.titleY();
            this.inventoryLabelX += m.labelX();
            this.inventoryLabelY += m.labelY();
        }
    }
}
