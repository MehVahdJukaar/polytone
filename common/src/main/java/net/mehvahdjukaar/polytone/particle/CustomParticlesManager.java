package net.mehvahdjukaar.polytone.particle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class CustomParticlesManager extends JsonPartialReloader {

    public final MapRegistry<CustomParticleFactory> customParticleFactories = new MapRegistry<>("Custom Particles");
    private final Map<ParticleType<?>, ParticleProvider<?>> overwrittenVanillaProviders = new HashMap<>();

    public static final Codec<CustomParticleFactory> CUSTOM_OR_SEMI_CUSTOM_CODEC = Codec.either(SemiCustomParticleType.CODEC, CustomParticleType.CODEC)
            .xmap(e -> e.map(Function.identity(), Function.identity()),
                    p -> p instanceof CustomParticleType c ? Either.right(c) : Either.left((SemiCustomParticleType) p));

    public CustomParticlesManager() {
        super("custom_particles");
    }


    //just gathers the custom models if any are there
    @Override
    public void earlyProcess(ResourceManager resourceManager) {
        var jsons = this.getJsonsInDirectories(resourceManager);
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            var model = CustomParticleType.CUSTOM_MODEL_ONLY_CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg ->
                            Polytone.LOGGER.error("Could not decode Custom Particle with json id {}\n error: {}", id, errorMsg))
                    .getFirst();
            model.ifPresent(Polytone::addCustomModel);
        }
    }

    @Override
    protected void resetWithLevel(boolean isLogOff) {
        for (var id : customParticleFactories.orderedKeys()) {
            PlatStuff.unregisterParticleProvider(id);
            PlatStuff.unregisterDynamic(BuiltInRegistries.PARTICLE_TYPE, id);
        }
        customParticleFactories.clear();
        for (var v : overwrittenVanillaProviders.entrySet()) {
            PlatStuff.setParticleProvider(v.getKey(), v.getValue());
        }
        overwrittenVanillaProviders.clear();
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
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {

    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops,
                                  RegistryAccess access) {
        ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;

        Set<CustomParticleType> customTypes = new HashSet<>();

        for (var j : jsons.entrySet()) {
            try {
                var json = j.getValue();
                var id = j.getKey();
                CustomParticleFactory factory;
                if (json instanceof JsonObject jo && jo.has("copy_from")) {
                    factory = SemiCustomParticleType.CODEC.decode(ops, json)
                            .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Semi Custom Particle Type with json id {} - error: {}",
                                    id, errorMsg)).getFirst();
                } else {
                    factory = CustomParticleType.CODEC.decode(ops, json)
                            .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Custom Particle Type with json id {} - error: {}",
                                    id, errorMsg)).getFirst();
                }
                factory.setSpriteSet(Minecraft.getInstance().particleEngine.spriteSets.get(id));

                if (factory instanceof CustomParticleType c) {
                    customTypes.add(c);
                }

                if (BuiltInRegistries.PARTICLE_TYPE.get(id) != null) {
                    ParticleType oldType = BuiltInRegistries.PARTICLE_TYPE.get(id);
                    Polytone.LOGGER.info("Overriding particle with id {}", id);
                    var oldFactory = PlatStuff.getParticleProvider(oldType);
                    overwrittenVanillaProviders.put(oldType, oldFactory);
                    //override vanilla particle
                    try {
                        particleEngine.register(oldType, factory);
                    } catch (Exception e) {
                        Polytone.LOGGER.error("Can't override existing particle with ID {}. Particle type not supported", id, e);
                    }
                    continue;
                } else {
                    customParticleFactories.register(id, factory);
                }


                Polytone.LOGGER.info("Registered Custom Particle {}", id);
            } catch (Exception e) {
                Polytone.LOGGER.error("!!!!!!!!!!!! Failed to load Custom Particle {}", j.getKey(), e);
            }
        }

        // register custom particle types. needs to be here
        for (var c : customParticleFactories.getEntries()) {
            var factory = c.getValue();
            var id = c.getKey();
            SimpleParticleType type = PlatStuff.makeParticleType(factory.forceSpawns());
            PlatStuff.registerDynamic(BuiltInRegistries.PARTICLE_TYPE, id, type);
            particleEngine.register(type, factory);
        }

        //initialize recursive stuff
        for (var c : customTypes) {
            for (var d : c.lazyParticles) {
                try {
                    c.particles.add(runCodec(ops, d));
                } catch (Exception e) {
                    Polytone.LOGGER.error("Failed to initialize custom particles particle emitters", e);
                }
            }
            c.lazyParticles = null;
        }
    }

    private static <T> ParticleParticleEmitter runCodec(DynamicOps o, Dynamic<T> dynamic) {
        DynamicOps<T> ops = (DynamicOps<T>) o;
        return ParticleParticleEmitter.CODEC.decode(ops, dynamic.getValue()).getOrThrow(false,
                Polytone.LOGGER::error).getFirst();
    }

    public Codec<CustomParticleFactory> byNameCodec() {
        return customParticleFactories;
    }
}
