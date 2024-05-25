package net.mehvahdjukaar.polytone.dimension;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.biome.BiomeEffectModifier;
import net.mehvahdjukaar.polytone.block.BlockPropertyModifier;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.mehvahdjukaar.polytone.utils.JsonImgPartialReloader;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DimensionEffectsManager extends JsonImgPartialReloader {

    private final Map<ResourceKey<Level>, DimensionEffectsModifier> dimensionEffects = new HashMap<>();

    private final Map<ResourceKey<Level>, DimensionEffectsModifier> vanillaEffects = new HashMap<>();


    public DimensionEffectsManager() {
        super("dimension_effects");
    }

    @Override
    protected void reset() {

    }

    @Override
    protected void process(Resources resources) {
        var jsons = resources.jsons();
        var textures = resources.textures();

        Set<ResourceLocation> usedTextures = new HashSet<>();

        Map<ResourceLocation, DimensionEffectsModifier> parsedModifiers = new HashMap<>();


        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            ResourceLocation id = j.getKey();

            DimensionEffectsModifier modifier = DimensionEffectsModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Dimension Effects with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            //always have priority
            if (parsedModifiers.containsKey(id)) {
                Polytone.LOGGER.warn("Found duplicate Dimension Effects file with id {}. This is likely a non .json converted legacy one" +
                        "Overriding previous one", id);
            }
            parsedModifiers.put(id, modifier);
        }

        // add all modifiers (with or without texture)
        for (var entry : parsedModifiers.entrySet()) {
            ResourceLocation id = entry.getKey();
            DimensionEffectsModifier modifier = entry.getValue();

            if (!modifier.hasColormap() && textures.containsKey(id)) {
                //if this map doesn't have a colormap defined, we set it to the default impl IF there's a texture it can use
                modifier = modifier.merge(DimensionEffectsModifier.ofFogColor(Colormap.defTriangle()));
            }

            //fill inline colormaps colormapTextures
            if (modifier.hasColormap()) {
                BlockColor tint = modifier.getColormap();
                if (tint instanceof Colormap c) {
                    var text = textures.get(c.getTargetTexture() == null ? id : c.getTargetTexture());
                    if (text != null) {
                        ColormapsManager.tryAcceptingTexture(text, id, c, usedTextures);
                    } else if (c.getTargetTexture() != null) {
                        Polytone.LOGGER.error("Could not resolve explicit texture at location {}.png for colormap from dimension effects file with it {}. Skipping", c.getTargetTexture(), id);
                        continue;
                    }
                }
            }
            addModifier(id, modifier);
        }

        // creates orphaned texture colormaps & properties
        textures.keySet().removeAll(usedTextures);

        for (var t : textures.entrySet()) {
            ResourceLocation id = t.getKey();
            Colormap defaultColormap = Colormap.defTriangle();
            ColormapsManager.tryAcceptingTexture(textures.get(id), id, defaultColormap, usedTextures);

            addModifier(id, DimensionEffectsModifier.ofFogColor(defaultColormap));
        }
    }

    private void addModifier(ResourceLocation fileId, DimensionEffectsModifier mod) {
        for (Block block : mod.getTargets(fileId, Registries.DIMENSION)) {
            DimensionSpecialEffects
            dimensionEffects.merge(block, mod, DimensionEffectsModifier::merge);
        }
    }

    @Nullable
    public Vec3 modifyFogColor(Vec3 center, ClientLevel level, int lightLevel) {
        var mod = this.modifiers.get(level.dimension());
        if (mod == null) return null;

       return mod.computeFogColor(center, level, lightLevel);
    }
}
