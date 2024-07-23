package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Inject(method = "addSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/NonNullList;add(Ljava/lang/Object;)Z",
            ordinal = 0),
            require = 1)
    public void interact(Slot slot, CallbackInfoReturnable<Slot> cir,
                         @Local(argsOnly = true) LocalRef<Slot> mutableSlot) {
        Polytone.SLOTIFY.maybeModifySlot((AbstractContainerMenu) (Object) this, slot);
    }
}
