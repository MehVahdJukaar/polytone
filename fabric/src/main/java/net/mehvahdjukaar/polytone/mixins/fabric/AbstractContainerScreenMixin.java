package net.mehvahdjukaar.polytone.mixins.fabric;

import com.google.common.base.Preconditions;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
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

    @Shadow
    @Nullable
    protected Slot hoveredSlot;
    @Shadow protected int topPos;
    @Shadow protected int leftPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;
    @Unique
    private Integer polytone$customLabelColor = null;
    @Unique
    private Integer polytone$customTitleColor = null;
    // Size offsets already applied. imageWidth/imageHeight/width/height persist across rebuildWidgets
    // (unlike the label/pos fields, which vanilla init recomputes), so we track and undo the delta to
    // keep re-applies idempotent for the live editor preview.
    @Unique
    private int polytone$appliedWOff = 0;
    @Unique
    private int polytone$appliedHOff = 0;

    protected AbstractContainerScreenMixin(Component component) {
        super(component);
    }

    @WrapWithCondition(method = "render", at = @At(
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V",
            value = "INVOKE"
    ))
    public boolean slotifyColor(GuiGraphics graphics, int x, int y, int blitOffset,
                                @Local Slot slot) {
        return Polytone.SLOTIFY.maybeChangeColor((AbstractContainerScreen<?>) (Object) this,
                Preconditions.checkNotNull(slot), graphics, x, y, blitOffset);
    }

    // Undo the previously applied size delta before vanilla init recomputes leftPos/labels from the
    // (now pristine) image size, so everything re-bakes from a clean base on every rebuildWidgets.
    @Inject(method = "init", at = @At("HEAD"))
    public void polytone$undoSizeOffsets(CallbackInfo ci) {
        this.imageWidth -= polytone$appliedWOff;
        this.imageHeight -= polytone$appliedHOff;
        this.width -= polytone$appliedWOff;
        this.height -= polytone$appliedHOff;
        polytone$appliedWOff = 0;
        polytone$appliedHOff = 0;
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void modifyLabels(CallbackInfo ci) {
        var m = Polytone.SLOTIFY.getGuiModifier((AbstractContainerScreen<?>) (Object) this);
        if (m != null) {
            // label/pos fields are recomputed fresh by vanilla init, so += stays idempotent
            this.titleLabelX += m.titleX();
            this.titleLabelY += m.titleY();
            this.inventoryLabelX += m.labelX();
            this.inventoryLabelY += m.labelY();
            this.polytone$customTitleColor = m.titleColor();
            this.polytone$customLabelColor = m.labelColor();
            this.leftPos += m.xOff();
            this.topPos += m.yOff();
            this.width += m.wOff();
            this.imageWidth += m.wOff();
            this.height += m.hOff();
            this.imageHeight += m.hOff();
            polytone$appliedWOff = m.wOff();
            polytone$appliedHOff = m.hOff();
        } else {
            this.polytone$customTitleColor = null;
            this.polytone$customLabelColor = null;
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
