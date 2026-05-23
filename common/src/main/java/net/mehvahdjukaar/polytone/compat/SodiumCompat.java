package net.mehvahdjukaar.polytone.compat;

import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;

public class SodiumCompat {
    public static @Nullable BlockAndTintGetter getLevel(@Nullable BlockAndTintGetter level) {
        if(!(level instanceof LevelReader)){
            //shid this could be bad
            return Minecraft.getInstance().level;
        }
        return level; //TODO:
    }
}
