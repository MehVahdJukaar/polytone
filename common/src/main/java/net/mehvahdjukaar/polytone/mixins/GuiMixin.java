package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Hud.class)
public abstract class GuiMixin {


    @WrapOperation(method = "extractHearts", at = @At(value = "INVOKE",
            ordinal = 0,
            target = "Lnet/minecraft/client/gui/Hud;extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Hud$HeartType;IIZZZ)V"))
    public void polytone$renderFancyEmptyHeart(Hud instance, GuiGraphicsExtractor graphics, Hud.HeartType empty, int i, int j,
                                               boolean bl, boolean bl2, boolean bl3, Operation<Void> original,
                                               @Local Hud.HeartType actualType) {
        if (Polytone.OVERLAY_MODIFIERS.maybeFancifyHeart(instance, graphics, actualType, i, j, bl, bl2, bl3)) {
            return;
        }
        original.call(instance, graphics, empty, i, j, bl, bl2, bl3);
    }
}
