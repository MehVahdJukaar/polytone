package net.mehvahdjukaar.polytone.content.particle.custom;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.SpecialModelsHandler;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.polytone.content.particle.ParticleParticleEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CustomParticlesManager extends ContentManager<ICustomParticleFactory> {

    public final MapRegistry<ICustomParticleFactory> customParticleFactories = new MapRegistry<>("Custom Particles");
    private final Map<ParticleType<?>, ParticleProvider<?>> overwrittenVanillaProviders = new HashMap<>();

    // The wire codec is the copy_from-dispatching one (NOT a try-both fold: a broken
    // semi-custom json must fail as semi-custom, not silently decode as custom, which
    // ignores unknown keys); labeled() splices editor labels on without touching the wire.
    public static final SchemaCodec<ICustomParticleFactory> CUSTOM_OR_SEMI_CUSTOM_CODEC = SchemaCodecs.labeled(
            CustomOrSemiCustomParticleCodec.INSTANCE,
            SchemaCodecs.alt("semi custom", SemiCustomParticleType.CODEC),
            SchemaCodecs.alt("custom", CustomParticleType.CODEC));

    public CustomParticlesManager() {
        super(Spec.of("Custom particle", () -> CUSTOM_OR_SEMI_CUSTOM_CODEC)
                .wikiPage("Custom-Particle-Types")
                .folders("custom_particles"));
    }


    //just gathers the custom models if any are there
    @Override
    public void earlyProcess(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        var jsons = this.getJsonsInDirectories(resourceManager);
        for (var j : jsons.entrySet()) {
            JsonElement json = j.getValue();
            Identifier id = j.getKey();
            var model = CustomParticleType.CUSTOM_MODEL_ONLY_CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Custom Particle with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
            model.ifPresent(SpecialModelsHandler::addSpecialModel);
        }
    }

    @Override
    protected void resetWithLevel(boolean isLogOff) {
        for (var id : customParticleFactories.orderedKeys()) {
            ICustomParticleFactory p = customParticleFactories.getValue(id);
            if (p instanceof CustomParticleType cp) {
                cp.setUnregistered();
            }
            PlatStuff.unregisterParticleProvider(id);
            PlatStuff.unregisterDynamic(BuiltInRegistries.PARTICLE_TYPE, id);
        }
        customParticleFactories.clear();
        ParticleResources resources = Minecraft.getInstance().particleEngine.resourceManager;
        for (var v : overwrittenVanillaProviders.entrySet()) {
            ParticleType<?> key = v.getKey();
            PlatStuff.setParticleProvider(key, v.getValue());
            //reset sprite sets
            Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(key);
            if (id != null) {
                //resources.spriteSets.put(id, new ParticleResources.MutableSpriteSet());
            }
        }
        overwrittenVanillaProviders.clear();
    }

    // not ideal. early process or something
    public void addSpriteSets(ResourceManager resourceManager) {
        ParticleResources resources = Minecraft.getInstance().particleEngine.resourceManager;
        for (var v : customParticleFactories.keySet()) {
            //never remove them as it could crash with old already spawner particles. not ideal
            resources.spriteSets.remove(v);
        }
        try {
            var jsons = this.getJsonsInDirectories(resourceManager);
            for (Identifier v : jsons.keySet()) {
                //just add missing ones
                resources.spriteSets.computeIfAbsent(v, a -> new ParticleResources.MutableSpriteSet());
            }
        }catch (Exception e){
            Polytone.LOGGER.error("Error adding custom particle sprite sets from resource packs",e);
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops,
                                  HolderLookup.Provider access) {
        Map<Identifier, JsonElement> jsons = resources.jsons();
        ParticleResources particleResources = Minecraft.getInstance().particleEngine.resourceManager;

        Set<CustomParticleType> customTypes = new HashSet<>();


        for (var j : parseEnabledJsons(jsons, ops)) {
            var factory = j.getValue();
            Identifier id = j.getKey();

            if (factory instanceof CustomParticleType c) {
                customTypes.add(c);
                c.debugId = id;
            }

            ParticleResources.MutableSpriteSet spriteSet = particleResources.spriteSets.get(id);
            factory.setSpriteSet(spriteSet);

            if (BuiltInRegistries.PARTICLE_TYPE.get(id).isPresent()) {
                ParticleType<?> oldType = BuiltInRegistries.PARTICLE_TYPE.get(id).get().value();
                var oldFactory = PlatStuff.getParticleProvider(oldType);
                //override vanilla particle
                try {
                    particleResources.register(oldType, new OverridingParticleFactory<>(factory));
                    overwrittenVanillaProviders.put(oldType, oldFactory);
                } catch (Exception e) {
                    Polytone.LOGGER.error("Can't override existing particle with ID {}. Particle type not supported", id, e);
                }

            } else {
                ParticleType<ParticleOptions> type = PlatStuff.makeParticleType(factory.forceSpawns());
                PlatStuff.registerDynamic(BuiltInRegistries.PARTICLE_TYPE, id, type);
                particleResources.register(type, factory);
                customParticleFactories.register(id, factory);
            }
        }

        logBatch("Registered custom particles: ",
                customParticleFactories.keySet());
        logBatch("Overridden vanilla particles: ",
                overwrittenVanillaProviders.keySet()
                        .stream().map(BuiltInRegistries.PARTICLE_TYPE::getKey).toList());

        //initialize recursive stuff
        for (var c : customTypes) {
            if (c.lazyEmitters != null) {
                for (var d : c.lazyEmitters) {
                    c.particleEmitters.add(runCodec(ops, d));
                }
                c.lazyEmitters = null;
            }
        }
    }

    private static <T> void logBatch(String prefix, Iterable<T> entries) {
        StringBuilder sb = new StringBuilder(prefix);
        boolean first = true;

        for (T entry : entries) {
            if (entry == null) continue;

            if (!first) sb.append(", ");
            sb.append(entry);
            first = false;
        }

        if (!first) { // at least one entry was appended
           Polytone.LOGGER.info(sb.toString());
        }
    }

    private static <T> ParticleParticleEmitter runCodec(DynamicOps o, Dynamic<T> dynamic) {
        DynamicOps<T> ops = (DynamicOps<T>) o;
        return ParticleParticleEmitter.CODEC.decode(ops, dynamic.getValue()).getOrThrow().getFirst();
    }

    public Codec<ICustomParticleFactory> byNameCodec() {
        return customParticleFactories;
    }

    // GPU particle types too: same dynamic registration, same emitter overrides
    public boolean isDynamicParticle(Identifier entryId) {
        return customParticleFactories.containsKey(entryId) || Polytone.GPU_PARTICLES.isGpuParticle(entryId);
    }

    @Nullable
    public Identifier getId(CustomParticleType customParticleType) {
        return customParticleFactories.getKey(customParticleType);
    }
}
