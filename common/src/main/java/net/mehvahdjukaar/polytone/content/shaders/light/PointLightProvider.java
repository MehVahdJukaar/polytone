package net.mehvahdjukaar.polytone.content.shaders.light;

import net.mehvahdjukaar.polytone.api.ResolvedPointLight;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

//packs give a ColoredLight, mods a plain function. null = not lit right now
public interface PointLightProvider<T> {

    @Nullable
    ResolvedPointLight resolve(T thing, ClientLevel level, float defaultRadius);

    interface ForBlock {
        @Nullable
        ResolvedPointLight resolve(BlockState state, Vec3 pos, ClientLevel level, float defaultRadius);
    }

    interface ForItem {
        @Nullable
        ResolvedPointLight resolve(ItemStack stack, Entity holder, ClientLevel level, float defaultRadius);
    }
}
