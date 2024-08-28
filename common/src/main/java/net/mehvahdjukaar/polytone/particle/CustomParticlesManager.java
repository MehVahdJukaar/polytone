package net.mehvahdjukaar.polytone.particle;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;
import java.util.function.Function;

public class CustomParticlesManager extends JsonPartialReloader {

    public final MapRegistry<CustomParticleFactory> customParticleFactories = new MapRegistry<>("Custom Particles");

    public static final Codec<CustomParticleFactory> CUSTOM_OR_SEMI_CUSTOM_CODEC = Codec.either(SemiCustomParticleType.CODEC, CustomParticleType.CODEC)
            .xmap(e -> e.map(Function.identity(), Function.identity()),
                    p -> p instanceof CustomParticleType c ? Either.right(c) : Either.left((SemiCustomParticleType) p));

    public CustomParticlesManager() {
        super("custom_particles");
    }

    @Override
    protected void reset() {
        for (var id : customParticleFactories.orderedKeys()) {
            PlatStuff.unregisterParticleProvider(id);
            PlatStuff.unregisterDynamic(BuiltInRegistries.PARTICLE_TYPE, id);
        }
        customParticleFactories.clear();
    }

    // not ideal
    public void addSpriteSets(ResourceManager resourceManager) {
        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        for (var v : customParticleFactories.keySet()) {
            engine.spriteSets.remove(v);
        }
        var jsons = this.getJsonsInDirectories(resourceManager);
        this.checkConditions(jsons);
        for (var v : jsons.keySet()) {
            engine.spriteSets.put(v, new ParticleEngine.MutableSpriteSet());
        }
    }

    @Override
    protected void process(Map<ResourceLocation, JsonElement> obj, DynamicOps<JsonElement> ops) {
        ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;

        for (var j : obj.entrySet()) {
            try {
                var json = j.getValue();
                var id = j.getKey();
                CustomParticleFactory factory = CUSTOM_OR_SEMI_CUSTOM_CODEC.decode(ops, json)
                        .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Custom Particle with json id " + id + "\n error: " + errorMsg))
                        .getFirst();
                factory.setSpriteSet(Minecraft.getInstance().particleEngine.spriteSets.get(id));

                customParticleFactories.register(id, factory);

                SimpleParticleType type = PlatStuff.makeParticleType();
                PlatStuff.registerDynamic(BuiltInRegistries.PARTICLE_TYPE, id, type);

                particleEngine.register(type, factory);
                Polytone.LOGGER.info("Registered Custom Particle {}", id);
            }catch (Exception e){
                Polytone.LOGGER.error("!!!!!!!!!!!! Failed to load Custom Particle {}", j.getKey(), e);
            }
        }
    }

    public Codec<CustomParticleFactory> byNameCodec() {
        return customParticleFactories;
    }
}
