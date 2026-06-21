package net.mehvahdjukaar.polytone.mixins.neoforge;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.polytone.content.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.content.item.ItemModifier;
import net.mehvahdjukaar.polytone.content.item.PolytoneClientItemExtensions;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Routes items that carry a Polytone modifier with client extension data through {@link PolytoneClientItemExtensions}.
 * We modify the {@code of(Item)} return value (instead of cancelling at HEAD) so we <i>wrap</i> whatever extension was
 * already resolved for the item, the vanilla {@code DEFAULT} or another mod's, and only override the hooks we drive.
 * {@code remap = false}: these are NeoForge symbols, not Minecraft.
 */
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
