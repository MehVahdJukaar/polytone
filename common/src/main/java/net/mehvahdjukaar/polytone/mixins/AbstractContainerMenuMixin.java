package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Shadow @Final public NonNullList<Slot> slots;

    @Inject(method = "addSlot", at = @At(value = "HEAD"))
    public void polytone$modifySlot(Slot slot, CallbackInfoReturnable<Slot> cir) {
        slot.index = this.slots.size(); //dumb because injecting at nonnulllist.add never works in prod
        Polytone.SLOTIFY.maybeModifySlot((AbstractContainerMenu) (Object) this, slot);
    }
}
