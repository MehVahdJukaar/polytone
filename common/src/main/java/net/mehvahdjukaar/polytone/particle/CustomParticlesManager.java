package net.mehvahdjukaar.polytone.particle;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;

import java.util.Map;

public class CustomParticlesManager extends JsonPartialReloader {

    private static final BiMap<String, CustomParticleType> CUSTOM_PARTICLES = HashBiMap.create();

    public static final Codec<CustomParticleType> REFERENCE_CODEC = ExtraCodecs.stringResolverCodec(
            a -> CUSTOM_PARTICLES.inverse().get(a), CUSTOM_PARTICLES::get);

    public CustomParticlesManager() {
        super("custom_particles");
    }

    @Override
    protected void reset() {
        CUSTOM_PARTICLES.clear();
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager) {
        var map = super.prepare(resourceManager);
        // super hacky
        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        for (var v : CUSTOM_PARTICLES.keySet()) {
            engine.spriteSets.remove(new ResourceLocation(v));
        }
        for (var v : map.keySet()) {
            engine.spriteSets.put(v, new ParticleEngine.MutableSpriteSet());
        }
        return map;
    }

    @Override
    protected void process(Map<ResourceLocation, JsonElement> obj) {
        for (var j : obj.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            var type = CustomParticleType.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Custom Particle Type with json id {} - error: {}",
                            id, errorMsg)).getFirst();
            type.setSpriteSet(Minecraft.getInstance().particleEngine.spriteSets.get(id));

            CUSTOM_PARTICLES.put(id.getPath(), type);
        }
    }
}
