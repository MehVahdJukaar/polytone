package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.item.ItemModifier;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Item.class)
public abstract class ItemMixin implements IPolytoneItem {

    @Unique
    private ItemModifier polytone$modifier;

    @Override
    public ItemModifier polytone$getModifier() {
        return polytone$modifier;
    }

    @Override
    public void polytone$setModifier(ItemModifier modifier) {
        this.polytone$modifier = modifier;
    }
}
