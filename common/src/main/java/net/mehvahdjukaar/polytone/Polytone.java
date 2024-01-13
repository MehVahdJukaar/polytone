package net.mehvahdjukaar.polytone;

import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.polytone.biome.BiomeEffectsManager;
import net.mehvahdjukaar.polytone.color.ColorManager;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class Polytone {
    public static final String MOD_ID = "polytone";

    public static final Logger LOGGER = LogManager.getLogger("Polytone");

    public static void init(boolean fabric) {

        PlatStuff.addClientReloadListener(ColorManager::new, res("color_manager"));
        PlatStuff.addClientReloadListener(PropertiesReloadListener::new, res("block_properties_manager"));
    }

    public static ResourceLocation res(String name) {
        return new ResourceLocation(MOD_ID, name);
    }


    public static void onTagsReceived(RegistryAccess registryAccess) {
        BiomeEffectsManager.doApply(registryAccess, true);

    }

    public static ResourceLocation getLocalId(ResourceLocation path) {
        //TODO: this is unconventional and bad
        return new ResourceLocation(path.getPath().replaceFirst("/", ":"));
    }

    // gets a target either at local path or global one
    @Nullable
    public static <T> Pair<T, ResourceLocation> getTarget(ResourceLocation resourcePath, Registry<T> registry) {
        ResourceLocation id = getLocalId(resourcePath);
        var opt = registry.getOptional(id);
        if (opt.isPresent()) return Pair.of(opt.get(), id);
        opt = registry.getOptional(resourcePath);
        return opt.map(t -> Pair.of(t, resourcePath)).orElse(null);
    }


}
