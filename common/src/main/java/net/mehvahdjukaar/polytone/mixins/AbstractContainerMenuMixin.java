package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.mehvahdjukaar.polytone.slotify.GuiModifierManager;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {


    @Shadow
    @Final
    @Nullable
    private MenuType<?> menuType;

    @Inject(method = "addSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/NonNullList;add(Ljava/lang/Object;)Z",
            ordinal = 0),
            require = 1)
    public void interact(Slot slot, CallbackInfoReturnable<Slot> cir,
                         @Local LocalRef<Slot> mutableSlot) {
        GuiModifierManager.maybeModifySlot((AbstractContainerMenu)(Object)this, slot);
    }
}
