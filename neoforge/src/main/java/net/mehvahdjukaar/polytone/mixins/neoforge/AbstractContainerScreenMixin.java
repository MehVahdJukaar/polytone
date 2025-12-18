package net.mehvahdjukaar.polytone.mixins.neoforge;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
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
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;
    @Unique
    private Integer polytone$customTitleColor;
    @Unique
    private Integer polytone$customLabelColor;

    protected AbstractContainerScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void polytone$modifyLabels(CallbackInfo ci) {
        var m = Polytone.SLOTIFY.getGuiModifier(this);
        if (m != null) {
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
        }
    }

    @ModifyArg(method = "renderLabels",
            index = 4,
            require = 1,
            at = @At(value = "INVOKE",
                    ordinal = 0,
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"))
    private int polytone$changeTitleColor(int fontColor) {
        if (polytone$customTitleColor != null) return polytone$customTitleColor;
        return fontColor;
    }


    @ModifyArg(method = "renderLabels",
            index = 4,
            require = 1,
            at = @At(value = "INVOKE",
                    ordinal = 1,
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"))
    private int polytone$changeLabelColor(int fontColor) {
        if (polytone$customLabelColor != null) return polytone$customLabelColor;
        return fontColor;
    }
}
