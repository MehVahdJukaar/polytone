package net.mehvahdjukaar.polytone.mixins.accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int polytone$getLeftPos();

    @Accessor("topPos")
    int polytone$getTopPos();
}
