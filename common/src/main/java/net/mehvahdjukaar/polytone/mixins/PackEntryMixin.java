package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfo;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfoScreen;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TransferableSelectionList.PackEntry.class)
public abstract class PackEntryMixin extends  ObjectSelectionList.Entry<TransferableSelectionList.Entry>{

    @Unique
    private static final Identifier POLYTONE$BADGE = Polytone.res("pack_info");
    @Unique
    private static final Identifier POLYTONE$BADGE_HIGHLIGHTED = Polytone.res("pack_info_highlighted");
    @Unique
    private static final int POLYTONE$BADGE_SIZE = 8;
    /** Gap kept between the heart and the entry's right edge, and between the heart and the title. */
    @Unique
    private static final int POLYTONE$BADGE_MARGIN = 2;

    @Shadow
    @Final
    private PackSelectionModel.Entry pack;

    @Unique
    private @Nullable PackInfo polytone$info() {
        if (!this.pack.isSelected()) return null;
        return PackInfos.get(this.pack.getId());
    }

    @Unique
    private int polytone$badgeX() {
        return this.getContentRight() - POLYTONE$BADGE_SIZE - POLYTONE$BADGE_MARGIN;
    }

    @Unique
    private int polytone$badgeY() {
        return this.getContentY() + 1;
    }

    @Unique
    private boolean polytone$isOverBadge(double mouseX, double mouseY) {
        if (polytone$info() == null) return false;
        int x = polytone$badgeX();
        int y = polytone$badgeY();
        return mouseX >= x && mouseX < x + POLYTONE$BADGE_SIZE && mouseY >= y && mouseY < y + POLYTONE$BADGE_SIZE;
    }

    // long pack names would otherwise run straight under the heart. only the title widget is a
    // StringWidget, the description below is a MultiLineTextWidget, so this only hits the title
    @ModifyArg(method = "extractContent", index = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/StringWidget;setMaxWidth(I)Lnet/minecraft/client/gui/components/StringWidget;"))
    private int polytone$narrowTitleForBadge(int maxWidth) {
        if (polytone$info() == null) return maxWidth;
        return maxWidth - POLYTONE$BADGE_SIZE - POLYTONE$BADGE_MARGIN * 2;
    }

    @Inject(method = "extractContent", at = @At("TAIL"))
    private void polytone$renderBadge(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering,
                                      float partialTick, CallbackInfo ci) {
        if (polytone$info() == null) return;

        boolean over = polytone$isOverBadge(mouseX, mouseY);
        if (over) {
            graphics.setTooltipForNextFrame(Component.translatable("screen.polytone.pack_info.tooltip"), mouseX, mouseY);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, over ? POLYTONE$BADGE_HIGHLIGHTED : POLYTONE$BADGE,
                polytone$badgeX(), polytone$badgeY(), POLYTONE$BADGE_SIZE, POLYTONE$BADGE_SIZE);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void polytone$clickBadge(MouseButtonEvent event, boolean doubleClicked, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0 || !polytone$isOverBadge(event.x(), event.y())) return;
        PackInfo info = polytone$info();
        if (info == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        minecraft.setScreen(new PackInfoScreen(minecraft.screen, this.pack.getTitle(), info));
        cir.setReturnValue(true);
    }
}
