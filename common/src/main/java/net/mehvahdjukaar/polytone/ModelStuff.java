package net.mehvahdjukaar.polytone;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class ModelStuff {

    @ExpectPlatform
    public static void clear() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addSpecialModel(Identifier id) {
        throw new AssertionError();
    }

    @Nullable
    public static QuadCollection getSpecialModel(Identifier id) {
        throw new AssertionError();
    }
}
