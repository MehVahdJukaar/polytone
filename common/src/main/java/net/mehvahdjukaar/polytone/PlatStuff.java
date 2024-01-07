package net.mehvahdjukaar.polytone;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class PlatStuff {
    @ExpectPlatform
    public static boolean isModStateValid() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addClientReloadListener(final Supplier<PreparableReloadListener> listener, final ResourceLocation name) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static BlockColor getBlockColor(BlockColors colors, Block block) {
        throw new AssertionError();
    }
}
