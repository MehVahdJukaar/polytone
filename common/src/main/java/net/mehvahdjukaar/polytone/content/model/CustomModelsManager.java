package net.mehvahdjukaar.polytone.content.model;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads data driven entity model geometry from {@code polytone/custom_models/} and exposes it both as a
 * referenceable registry (for item modifiers, by name or inlined) and as a set of real {@link LayerDefinition}s
 * keyed by {@link ModelLayerLocation}. Those layers are merged into the game's {@code EntityModelSet} so they bake
 * through the vanilla pipeline, which makes them visible to model replacing mods such as EMF.
 */
public class CustomModelsManager extends JsonPartialReloader {

    /** Sub layer name used for every Polytone model layer. */
    public static final String LAYER = "main";

    private final MapRegistry<ModelDefinition> models = new MapRegistry<>("Polytone Custom Models");
    private final Map<ModelLayerLocation, LayerDefinition> layers = new HashMap<>();

    public CustomModelsManager() {
        super("custom_models");
    }

    /** Reference-by-id codec, used by {@code CodecUtils.referenceOrDirect} alongside {@link ModelDefinition#CODEC}. */
    public Codec<ModelDefinition> byNameCodec() {
        return models;
    }

    public static ModelLayerLocation layerLocation(ResourceLocation id) {
        return new ModelLayerLocation(id, LAYER);
    }

    /** Live view of the data driven layers, read by the {@code EntityModelSet} mixin when baking. */
    public Map<ModelLayerLocation, LayerDefinition> getLayers() {
        return layers;
    }

    public boolean isEmpty() {
        return layers.isEmpty();
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> obj, RegistryOps<JsonElement> ops, RegistryAccess access) {
        load(obj, ops);
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        // layers are already built during parse; nothing level dependent to apply
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        models.clear();
        layers.clear();
    }

    private void load(Map<ResourceLocation, JsonElement> jsons, DynamicOps<JsonElement> ops) {
        models.clear();
        layers.clear();
        for (var e : jsons.entrySet()) {
            ResourceLocation id = e.getKey();
            ModelDefinition def = ModelDefinition.CODEC.decode(ops, e.getValue())
                    .getOrThrow(err -> new IllegalStateException("Could not decode Custom Model with id " + id + "\n error: " + err))
                    .getFirst();
            models.register(id, def);
            try {
                layers.put(layerLocation(id), def.toLayerDefinition());
            } catch (Exception ex) {
                Polytone.LOGGER.error("Failed to build model layer for custom model {}", id, ex);
            }
        }
        if (!models.isEmpty()) {
            Polytone.LOGGER.info("Loaded {} Polytone Custom Models", models.size());
        }
    }
}
