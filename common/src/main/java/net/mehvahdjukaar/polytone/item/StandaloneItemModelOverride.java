package net.mehvahdjukaar.polytone.item;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public class StandaloneItemModelOverride extends ItemModelOverride {

    public static final Codec<StandaloneItemModelOverride> CODEC = Codec.<ItemModelOverride, Pair<Item, Boolean>>pair(
            ItemModelOverride.CODEC,
            RecordCodecBuilder.create(i -> i.group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Pair::getFirst),
                    Codec.BOOL.fieldOf("auto_model").forGetter(Pair::getSecond)
            ).apply(i, Pair::of))
    ).xmap(
            pair -> new StandaloneItemModelOverride(pair.getFirst(), pair.getSecond().getFirst(), pair.getSecond().getSecond()),
            model -> Pair.of(model, Pair.of(model.getTarget(), model.isAutoModel())));

    private final Item item;
    private final boolean autoModel;

    public StandaloneItemModelOverride(ItemModelOverride parent, Item target, boolean autoModel) {
        super(parent.components(), parent.model());
        this.item = target;
        this.autoModel = autoModel;
    }


    public Item getTarget() {
        return item;
    }

    public boolean isAutoModel() {
        return autoModel;
    }
}
