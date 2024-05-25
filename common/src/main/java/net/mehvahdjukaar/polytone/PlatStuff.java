package net.mehvahdjukaar.polytone;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Contract;

import java.util.function.Supplier;

public class PlatStuff {
    @ExpectPlatform
    public static String maybeRemapName(String s) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static boolean isModStateValid() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addClientReloadListener(final Supplier<PreparableReloadListener> listener, final ResourceLocation name) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static BlockColor getBlockColor(BlockColors colors, Block block) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static boolean isModLoaded(String namespace) {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Contract
    public static SoundEvent registerSoundEvent(ResourceLocation id) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static DimensionSpecialEffects getDimensionEffects(ResourceLocation type) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void applyBiomeSurgery(Biome biome, BiomeSpecialEffects newEffects) {
        throw new AssertionError();
    }
}
