package net.mehvahdjukaar.polytone.block;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;

import java.util.*;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

public class BlockPropertiesManager extends JsonImgPartialReloader {

    private final Map<Block, BlockPropertyModifier> vanillaProperties = new HashMap<>();

    private final Map<ResourceLocation, BlockPropertyModifier> modifiers = new HashMap<>();

    public BlockPropertiesManager() {
        super("block_properties");
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        scanDirectory(resourceManager, path(), GSON, jsons);

        var textures = ArrayImage.gatherImages(resourceManager, path());

        var ofTextures = ArrayImage.gatherImages(resourceManager, "optifine/colormap");



        return new Resources(jsons, textures);
    }

    @Override
    public void process(Resources resources) {

        var jsons = resources.jsons();
        var textures = ArrayImage.groupTextures(resources.textures());

        Set<ResourceLocation> usedTextures = new HashSet<>();

        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            BlockPropertyModifier prop = BlockPropertyModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Client Block Property with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            //fill inline colormaps colormapTextures
            var colormap = prop.tintGetter();
            if (colormap.isPresent() && colormap.get() instanceof Colormap c && !c.isReference()) {
                ColormapsManager.fillColormapPalette(textures, id, c, usedTextures);
            }

            modifiers.put(id, prop);
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.createDefault(t.getValue().keySet());
            ColormapsManager.fillColormapPalette(textures, id, defaultColormap, usedTextures);

            BlockPropertyModifier defaultProp = new BlockPropertyModifier(Optional.of(defaultColormap),
                    Optional.empty(), Optional.empty(), Optional.empty());

            modifiers.put(id, defaultProp);
        }
    }

    @Override
    public void apply() {
        for (var p : modifiers.entrySet()) {
            var block = Polytone.getTarget(p.getKey(), BuiltInRegistries.BLOCK);
            if (block != null) {
                Block b = block.getFirst();
                BlockPropertyModifier value = p.getValue();
                vanillaProperties.put(b, value.apply(b));
            }
        }
        if (!vanillaProperties.isEmpty())
            Polytone.LOGGER.info("Applied {} Custom Block Properties", vanillaProperties.size());

        modifiers.clear();
    }

    @Override
    public void reset() {
        for (var e : vanillaProperties.entrySet()) {
            e.getValue().apply(e.getKey());
        }
        vanillaProperties.clear();
    }
}
