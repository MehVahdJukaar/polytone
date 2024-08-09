package net.mehvahdjukaar.polytone.particle;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;
import java.util.function.Function;

public class CustomParticlesManager extends JsonPartialReloader {

    public final MapRegistry<CustomParticleFactory> customParticles = new MapRegistry<>("Custom Particles");

    public static final Codec<CustomParticleFactory> CUSTOM_OR_SEMI_CUSTOM_CODEC = Codec.either(SemiCustomParticleType.CODEC, CustomParticleType.CODEC)
            .xmap(e -> e.map(Function.identity(), Function.identity()),
                    p -> p instanceof CustomParticleType c ? Either.right(c) : Either.left((SemiCustomParticleType) p));

    public CustomParticlesManager() {
        super("custom_particles");
    }

    @Override
    protected void reset() {
        for(var id : customParticles.orderedKeys()){
            PlatStuff.unregisterParticleProvider(id);
            PlatStuff.unregisterDynamic(BuiltInRegistries.PARTICLE_TYPE, id);
        }
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
    protected void process(Map<ResourceLocation, JsonElement> obj, DynamicOps<JsonElement> ops) {

        for (var j : obj.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            CustomParticleFactory type = CUSTOM_OR_SEMI_CUSTOM_CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Custom Particle with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
            type.setSpriteSet(Minecraft.getInstance().particleEngine.spriteSets.get(id));

            customParticles.register(id, type);
        }
    }

    @Override
    protected void apply() {
        ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;

        for(var id : customParticles.orderedKeys()) {
            SimpleParticleType type = PlatStuff.makeParticleType();
            PlatStuff.registerDynamic(BuiltInRegistries.PARTICLE_TYPE, id, type);

            particleEngine.register(type,customParticles.getValue(id));
        }

    }

    public Codec<CustomParticleFactory> byNameCodec() {
        return customParticles;
    }
}
