package net.mehvahdjukaar.polytone.particle;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;

public class CustomParticlesManager extends JsonPartialReloader {

    public final MapRegistry<CustomParticleType> customParticles = new MapRegistry<>("Custom Particles");

    public CustomParticlesManager() {
        super("custom_particles");
    }

    @Override
    protected void reset() {
        customParticles.clear();
    }

    // not ideal
    public void addSpriteSets(ResourceManager resourceManager) {
        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        for (var v : customParticles.keySet()) {
            engine.spriteSets.remove(v);
        }
        var jsons = this.getJsonsInDirectories(resourceManager);
        this.checkConditions(jsons);
        for (var v : jsons.keySet()) {
            engine.spriteSets.put(v, new ParticleEngine.MutableSpriteSet());
        }
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

            customParticles.register(id, type);
        }
    }


}
