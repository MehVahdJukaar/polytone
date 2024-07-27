package net.mehvahdjukaar.polytone.mixins.neoforge;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(CreativeModeTab.class)
public interface CreativeTabAccessor {

    @Mutable
    @Accessor("displayName")
    void setDisplayName(Component component);

    @Mutable
    @Accessor("hasSearchBar")
    void setHasSearchBar(boolean component);


    @Mutable
    @Accessor("searchBarWidth")
    void setSearchBarWidth(int width);

    @Mutable
    @Accessor("tabsImage")
    void setTabsImage(ResourceLocation tabsImage);

    @Accessor("backgroundTexture")
    void setBackgroundTexture(ResourceLocation back);

    @Mutable
    @Accessor("tabsBefore")
    void setBeforeTabs(List<ResourceLocation> beforeTabs);

    @Mutable
    @Accessor("tabsAfter")
    void setAfterTabs(List<ResourceLocation> afterTabs);


    @Accessor("iconItemStack")
    void setIcon(ItemStack icon);


    @Accessor("canScroll")
    void setCanScroll(boolean canScroll);


    @Accessor("showTitle")
    void setShowTitle(boolean canScroll);
}
