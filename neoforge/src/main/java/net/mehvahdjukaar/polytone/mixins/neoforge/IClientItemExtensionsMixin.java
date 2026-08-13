package net.mehvahdjukaar.polytone.mixins.neoforge;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.content.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.content.item.ItemModifier;
import net.mehvahdjukaar.polytone.content.item.PolytoneClientItemExtensions;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Modifies the of(Item) return value instead of cancelling at HEAD, so we wrap whatever extension
// was already resolved for the item and only override the hooks we drive.
// remap = false: these are neoforge symbols, not minecraft ones.
@Mixin(IClientItemExtensions.class)
public interface IClientItemExtensionsMixin {

    @ModifyReturnValue(
            method = "of(Lnet/minecraft/world/item/Item;)Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;",
            at = @At("RETURN"), remap = false)
    private static IClientItemExtensions polytone$wrapExtensions(IClientItemExtensions original, Item item) {
        if (item instanceof IPolytoneItem pi) {
            ItemModifier mod = pi.polytone$getModifier();
            if (mod != null && mod.hasClientItemExtensions()) {
                return PolytoneClientItemExtensions.wrap(original);
            }
        }
        return original;
    }
}
