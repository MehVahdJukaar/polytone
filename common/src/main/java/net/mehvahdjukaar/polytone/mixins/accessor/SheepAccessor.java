package net.mehvahdjukaar.polytone.mixins.accessor;

import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Sheep.class)
public interface SheepAccessor {

    @Invoker()
    static int invokeCreateSheepColor(DyeColor color) {
        return 0;
    }
}
