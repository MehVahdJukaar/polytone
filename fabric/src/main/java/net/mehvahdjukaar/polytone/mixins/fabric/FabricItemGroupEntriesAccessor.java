package net.mehvahdjukaar.polytone.mixins.fabric;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(FabricCreativeModeTabOutput.class)
public interface FabricItemGroupEntriesAccessor {

    @Accessor
    List<ItemStack> getDisplayStacks();

    @Accessor
    List<ItemStack> getSearchTabStacks();

}
