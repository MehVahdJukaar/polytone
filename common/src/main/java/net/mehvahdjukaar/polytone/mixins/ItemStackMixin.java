package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.item.IPolytoneItem;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {


    @Shadow
    public abstract Item getItem();

    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    public void polytone$modifyBarColor(CallbackInfoReturnable<Integer> cir) {
        var mod = ((IPolytoneItem) this.getItem()).polytone$getModifier();
        if (mod != null) {
            Integer barColor = mod.getBarColor((ItemStack) (Object) this);
            if (barColor != null) cir.setReturnValue(barColor);
        }
    }

    @ModifyReturnValue(method = "isBarVisible", at = @At("RETURN"))
    public boolean polytone$barVisible(boolean visible) {
        if (!visible) return false;
        var mod = ((IPolytoneItem) this.getItem()).polytone$getModifier();
        if (mod != null) {
            Integer barColor = mod.getBarColor((ItemStack) (Object) this);
            return barColor == null || ARGB.alpha(barColor) != 0;
        }
        return true;
    }
}
