package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.tabs.TabContentsEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;

/**
 * Applies Polytone's creative tab modifiers (item additions, removals and reordering) directly on
 * the final tab contents, at the tail of {@code buildContents}.
 * <p>
 * We deliberately do NOT hook into the NeoForge {@code BuildCreativeModeTabContentsEvent} or the
 * Fabric {@code ItemGroupEvents}/{@code CreativeModeTabEvents}: those run at an undefined order
 * relative to other mods, so Polytone could end up reordering before another mod has added its
 * items. By injecting here with a higher-than-default priority, we are guaranteed to run after the
 * NeoForge event (fired inside this method body) and after Fabric API's own tail injection
 * (priority 1000), every single time. Category tabs are built before the search tab, so the search
 * tab still aggregates our modifications.
 */
@Mixin(value = CreativeModeTab.class, priority = 1500)
public abstract class CreativeModeTabMixin {

    @Shadow
    private Collection<ItemStack> displayItems;

    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @Inject(method = "buildContents", at = @At("TAIL"))
    private void polytone$modifyTabContents(CreativeModeTab.ItemDisplayParameters params, CallbackInfo ci) {
        CreativeModeTab self = (CreativeModeTab) (Object) this;
        ResourceKey<CreativeModeTab> key = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(self).orElse(null);
        if (key == null) return;
        Polytone.CREATIVE_TABS_MODIFIERS.modifyTab(
                new TabContentsEvent(key, displayItems, displayItemsSearchTab), params.holders());
    }
}
