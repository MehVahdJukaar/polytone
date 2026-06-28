package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ContextualBar.class)
public interface ContextualBarRendererMixin {

    @ModifyArg(method = "extractExperienceLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
    ordinal = 0), index = 4
    )
    private static int polytone$changeXpColorBack0(int color) {
        Integer newCol = Polytone.COLORS.getXpBarBackground();
        return newCol != null ? newCol : color;
    }

    @ModifyArg(method = "extractExperienceLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            ordinal = 1), index = 4
    )
    private static int polytone$changeXpColorBack1(int color) {
        Integer newCol = Polytone.COLORS.getXpBarBackground();
        return newCol != null ? newCol : color;
    }

    @ModifyArg(method = "extractExperienceLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            ordinal = 2), index = 4
    )
    private static int polytone$changeXpColorBack2(int color) {
        Integer newCol = Polytone.COLORS.getXpBarBackground();
        return newCol != null ? newCol : color;
    }

    @ModifyArg(method = "extractExperienceLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            ordinal = 3), index = 4
    )
    private static int polytone$changeXpColorBack3(int color) {
        Integer newCol = Polytone.COLORS.getXpBarBackground();
        return newCol != null ? newCol : color;
    }


    @ModifyArg(method = "extractExperienceLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            ordinal = 4), index = 4
    )
    private static int polytone$changeXpColor(int color) {
        Integer newCol = Polytone.COLORS.getXpBar();
        return newCol != null ? newCol : color;
    }


}
