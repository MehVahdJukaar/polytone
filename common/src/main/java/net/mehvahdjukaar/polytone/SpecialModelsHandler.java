package net.mehvahdjukaar.polytone;

import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
public class SpecialModelsHandler {

    @PlatformImpl
    public static void clear() {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void addSpecialModel(Identifier id) {
        throw new AssertionError();
    }

    @Contract
    @PlatformImpl
    @Nullable
    public static QuadCollection getSpecialModel(Identifier id) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static void finalizeAdditions() {
        throw new AssertionError();
    }
}
