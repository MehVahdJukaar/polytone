package net.mehvahdjukaar.polytone.particle;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.ModelStuff;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.mehvahdjukaar.polytone.utils.Parsed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
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
    public void earlyProcess(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        var jsons = this.getJsonsInDirectories(resourceManager);
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            var model = CustomParticleType.CUSTOM_MODEL_ONLY_CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Custom Particle with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
            model.ifPresent(ModelStuff::addSpecialModel);
        }
    }

    @Override
    protected void resetWithLevel(boolean isLogOff) {
        for (var id : customParticleFactories.orderedKeys()) {
            var p = customParticleFactories.getValue(id);
            if (p instanceof CustomParticleType cp) {
                cp.setUnregistered();
            }
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
        ParticleResources resources = Minecraft.getInstance().particleEngine.resourceManager;
        for (var v : customParticleFactories.keySet()) {
            //never remove them as it could crash with old already spawner particles. not ideal
            //resources.spriteSets.remove(v);
        }
        var jsons = this.getJsonsInDirectories(resourceManager);
        for (var v : jsons.keySet()) {

            resources.spriteSets.put(v, new ParticleResources.MutableSpriteSet());
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops,
                                  HolderLookup.Provider access) {
        ParticleResources particleResources = Minecraft.getInstance().particleEngine.resourceManager;

        Set<CustomParticleType> customTypes = new HashSet<>();

        for (var j : Parsed.batchParseOnlyEnabled(jsons, CustomOrSemiCustomParticleCodec.INSTANCE,
                ops, "custom particle")) {
            try {
                var factory = j.getValue();
                var id = j.getKey();
                factory.setSpriteSet(particleResources.spriteSets.get(id));

                if (factory instanceof CustomParticleType c) {
                    customTypes.add(c);
                }

                if (BuiltInRegistries.PARTICLE_TYPE.get(id).isPresent()) {
                    ParticleType<?> oldType = BuiltInRegistries.PARTICLE_TYPE.get(id).get().value();
                    Polytone.LOGGER.info("Overriding particle with id {}", id);
                    var oldFactory = PlatStuff.getParticleProvider(oldType);
                    overwrittenVanillaProviders.put(oldType, oldFactory);
                    //override vanilla particle
                    try {
                        particleResources.register(oldType, new OverridingParticleFactory<>(factory));
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
            ParticleType<ExtraDataParticleOptions> type = PlatStuff.makeParticleType(factory.forceSpawns());
            PlatStuff.registerDynamic(BuiltInRegistries.PARTICLE_TYPE, id, type);
            particleResources.register(type, factory);
        }

        //initialize recursive stuff
        for (var c : customTypes) {
            if(c.lazyParticles != null) {
                for (var d : c.lazyParticles) {
                    c.particles.add(runCodec(ops, d));
                }
                c.lazyParticles = null;
            }
        }
    }

    private static <T> ParticleParticleEmitter runCodec(DynamicOps o, Dynamic<T> dynamic) {
        DynamicOps<T> ops = (DynamicOps<T>) o;
        return ParticleParticleEmitter.CODEC.decode(ops, dynamic.getValue()).getOrThrow().getFirst();
    }

    public Codec<CustomParticleFactory> byNameCodec() {
        return customParticleFactories;
    }

    public boolean isDynamicParticle(ResourceLocation entryId) {
        return customParticleFactories.containsKey(entryId);
    }
}
