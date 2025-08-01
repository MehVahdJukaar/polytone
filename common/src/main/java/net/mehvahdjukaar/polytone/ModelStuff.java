package net.mehvahdjukaar.polytone;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.polytone.lightmap.LightmapsManager;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class ModelStuff {

    @ExpectPlatform
    public static void clear() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addSpecialModel(ResourceLocation id) {
        throw new AssertionError();
    }

    @Nullable
    public static QuadCollection getSpecialModel(ResourceLocation id) {
        throw new AssertionError();
    }
}
