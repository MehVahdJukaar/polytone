package net.mehvahdjukaar.polytone.mixins.fabric;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.slotify.GuiModifierManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {

    @Shadow
    protected int titleLabelX;

    @Shadow
    protected int titleLabelY;

    @Shadow
    protected int inventoryLabelX;

    @Shadow
    protected int inventoryLabelY;

    @Shadow @Nullable protected Slot hoveredSlot;
    @Unique
    private Integer polytone$customLabelColor = null;
    @Unique
    private Integer polytone$customTitleColor = null;

    protected AbstractContainerScreenMixin(Component component) {
        super(component);
    }

    @WrapWithCondition(method = "render", at = @At(
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlightFront(Lnet/minecraft/client/gui/GuiGraphics;)V",
            value = "INVOKE"
    ))
    public boolean slotifyColor(AbstractContainerScreen instance, GuiGraphics guiGraphics, @Local Slot slot) {
        if (this.hoveredSlot == null) return true;
        return Polytone.SLOTIFY.maybeChangeColor((AbstractContainerScreen<?>) (Object) this, slot, guiGraphics, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 0);
    }

    @WrapWithCondition(method = "render", at = @At(
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphics;)V",
            value = "INVOKE"
    ))
    public boolean slotifyColorBack(AbstractContainerScreen instance, GuiGraphics guiGraphics, @Local Slot slot) {
        if (this.hoveredSlot == null) return true;
        return Polytone.SLOTIFY.maybeChangeColor((AbstractContainerScreen<?>) (Object) this, slot, guiGraphics, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 0);
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void modifyLabels(CallbackInfo ci) {
        var m = Polytone.SLOTIFY.getGuiModifier((AbstractContainerScreen<?>) (Object) this);
        if (m != null) {
            this.titleLabelX += m.titleX();
            this.titleLabelY += m.titleY();
            this.inventoryLabelX += m.labelX();
            this.inventoryLabelY += m.labelY();
            this.polytone$customTitleColor = m.titleColor();
            this.polytone$customLabelColor = m.labelColor();
        }
    }

    @ModifyArg(method = "renderLabels",
            index = 4,
            require = 1,
            at = @At(value = "INVOKE",
                    ordinal = 0,
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"))
    private int changeTitleColor(int fontColor) {
        if (polytone$customTitleColor != null) return polytone$customTitleColor;
        return fontColor;
    }


    @ModifyArg(method = "renderLabels",
            index = 4,
            require = 1,
            at = @At(value = "INVOKE",
                    ordinal = 1,
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"))
    private int changeLabelColor(int fontColor) {
        if (polytone$customLabelColor != null) return polytone$customLabelColor;
        return fontColor;
    }
}
