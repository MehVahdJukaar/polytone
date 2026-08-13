package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfo;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfoScreen;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TransferableSelectionList.PackEntry.class)
public abstract class PackEntryMixin extends ObjectSelectionList.Entry<TransferableSelectionList.PackEntry> {

    @Unique
    private static final ResourceLocation POLYTONE$BADGE = Polytone.res("pack_info");
    @Unique
    private static final ResourceLocation POLYTONE$BADGE_HIGHLIGHTED = Polytone.res("pack_info_highlighted");
    @Unique
    private static final int POLYTONE$BADGE_SIZE = 8;
    // kept clear on the right of the row for the scrollbar gutter plus a small margin
    @Unique
    private static final int POLYTONE$BADGE_INSET = 10;
    // vanilla's own (private) MAX_NAME_WIDTH_PIXELS
    @Unique
    private static final int POLYTONE$MAX_NAME_WIDTH = 157;

    @Shadow
    @Final
    private PackSelectionModel.Entry pack;

    @Shadow
    @Final
    private FormattedCharSequence nameDisplayCache;

    // the row box is only handed to render(), so remember where the heart landed for the click test
    @Unique
    private int polytone$badgeX = Integer.MIN_VALUE;
    @Unique
    private int polytone$badgeY;
    @Unique
    private @Nullable FormattedCharSequence polytone$narrowedName;

    @Unique
    private @Nullable PackInfo polytone$info() {
        if (!this.pack.isSelected()) return null;
        return PackInfos.get(this.pack.getId());
    }

    @Unique
    private boolean polytone$isOverBadge(double mouseX, double mouseY) {
        if (this.polytone$badgeX == Integer.MIN_VALUE || polytone$info() == null) return false;
        return mouseX >= this.polytone$badgeX && mouseX < this.polytone$badgeX + POLYTONE$BADGE_SIZE
                && mouseY >= this.polytone$badgeY && mouseY < this.polytone$badgeY + POLYTONE$BADGE_SIZE;
    }

    // long pack names would otherwise run straight under the heart. the same call also draws the
    // "incompatible" label, so only the pack's own name cache gets swapped
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
    private int polytone$narrowTitleForBadge(GuiGraphics graphics, Font font, FormattedCharSequence text,
                                             int x, int y, int color, Operation<Integer> original) {
        if (text == this.nameDisplayCache && polytone$info() != null) {
            text = polytone$narrowedName(font);
        }
        return original.call(graphics, font, text, x, y, color);
    }

    @Unique
    private FormattedCharSequence polytone$narrowedName(Font font) {
        if (this.polytone$narrowedName == null) {
            Component title = this.pack.getTitle();
            int maxWidth = POLYTONE$MAX_NAME_WIDTH - POLYTONE$BADGE_SIZE - POLYTONE$BADGE_INSET;
            if (font.width(title) <= maxWidth) {
                this.polytone$narrowedName = title.getVisualOrderText();
            } else {
                FormattedText trimmed = FormattedText.composite(
                        font.substrByWidth(title, maxWidth - font.width("...")), FormattedText.of("..."));
                this.polytone$narrowedName = Language.getInstance().getVisualOrder(trimmed);
            }
        }
        return this.polytone$narrowedName;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void polytone$renderBadge(GuiGraphics graphics, int index, int top, int left, int width, int height,
                                      int mouseX, int mouseY, boolean hovering, float partialTick, CallbackInfo ci) {
        if (polytone$info() == null) return;

        this.polytone$badgeX = left + width - POLYTONE$BADGE_INSET - POLYTONE$BADGE_SIZE;
        this.polytone$badgeY = top + 1;

        boolean over = polytone$isOverBadge(mouseX, mouseY);
        graphics.blitSprite(over ? POLYTONE$BADGE_HIGHLIGHTED : POLYTONE$BADGE,
                this.polytone$badgeX, this.polytone$badgeY, POLYTONE$BADGE_SIZE, POLYTONE$BADGE_SIZE);
        if (over) {
            Screen screen = Minecraft.getInstance().screen;
            if (screen != null) {
                screen.setTooltipForNextRenderPass(Component.translatable("screen.polytone.pack_info.tooltip"));
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void polytone$clickBadge(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0 || !polytone$isOverBadge(mouseX, mouseY)) return;
        PackInfo info = polytone$info();
        if (info == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        minecraft.setScreen(new PackInfoScreen(minecraft.screen, this.pack.getTitle(), info));
        cir.setReturnValue(true);
    }
}
