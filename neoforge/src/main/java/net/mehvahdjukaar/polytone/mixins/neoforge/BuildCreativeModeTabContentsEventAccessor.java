package net.mehvahdjukaar.polytone.mixins.neoforge;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.InsertableLinkedOpenCustomHashSet;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BuildCreativeModeTabContentsEvent.class)
public interface BuildCreativeModeTabContentsEventAccessor {

    @Accessor("parentEntries")
    InsertableLinkedOpenCustomHashSet<ItemStack> getParentEntries();


    @Accessor("searchEntries")
    InsertableLinkedOpenCustomHashSet<ItemStack> getSearchEntries();
}
