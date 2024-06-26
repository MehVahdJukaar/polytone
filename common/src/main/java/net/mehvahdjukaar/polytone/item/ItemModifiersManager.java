package net.mehvahdjukaar.polytone.item;

import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;

public class ItemModifiersManager extends JsonImgPartialReloader {

    private final Map<Item, ItemModifier> modifiers = new HashMap<>();


    public ItemModifiersManager() {
        super("item_modifiers", "item_properties");
    }

    @Override
    protected void reset() {

    }

    @Override
    protected void process(Resources obj) {

    }
}
