package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @ModifyArg(method = "renderExperienceBar", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
    ordinal = 4), index = 4)
    public int polytone$changeXpColor(Font font, @Nullable String text, int x, int y, int color, boolean dropShadow) {
        return Polytone.COLORS.getXpBar();
    }
}
