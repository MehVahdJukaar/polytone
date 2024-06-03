package net.mehvahdjukaar.polytone.mixins.fabric;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeModeTab.class)
public interface CreativeTabAccessor {

    @Mutable
    @Accessor("displayName")
    void setDisplayName(Component component);


    @Accessor("iconItemStack")
    void setIcon(ItemStack icon);


    @Accessor("canScroll")
    void setCanScroll(boolean canScroll);


    @Accessor("showTitle")
    void setShowTitle(boolean canScroll);
}
