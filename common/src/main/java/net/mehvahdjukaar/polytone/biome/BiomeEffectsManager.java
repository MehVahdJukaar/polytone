package net.mehvahdjukaar.polytone.biome;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.block.BlockPropertyModifier;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BiomeEffectsManager extends JsonPartialReloader {

    private final Map<ResourceLocation, BiomeSpecialEffects> vanillaEffects = new HashMap<>();

    private final Map<ResourceLocation, BiomeEffectModifier> effectsToApply = new HashMap<>();
    private boolean needsDynamicApplication = true;

    public BiomeEffectsManager() {
        super("biome_modifiers", "biome_effects");
    }

    private final Map<ResourceLocation, JsonElement> lazyJsons = new HashMap<>();

    private final Map<Biome, BiomeEffectModifier> fogParametersModifiers = new HashMap<>();

    @Override
    public void process(Map<ResourceLocation, JsonElement> biomesJsons, DynamicOps<JsonElement> ops) {
        lazyJsons.clear();
        lazyJsons.putAll(biomesJsons);
        //else apply as soon as we load a level
    }

    // we need registry ops here since special effects use registry stuff...
    @Override
    public void applyWithLevel(RegistryAccess access, boolean firstLogin) {
        for (var j : lazyJsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            BiomeEffectModifier effect = BiomeEffectModifier.CODEC.decode(
                            RegistryOps.create(JsonOps.INSTANCE, access), json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Biome Special Effect with json id " + id + "\n error: " + errorMsg))
                    .getFirst();

            addEffect(id, effect, access);

        }
        lazyJsons.clear();

        doApplyWithLevel(access, firstLogin);
    }

    private void addEffect(ResourceLocation pathId, BiomeEffectModifier mod,RegistryAccess access) {
        Registry<Biome> registry = access.registryOrThrow(Registries.BIOME);
        for (var biome : mod.getTargets(pathId, registry)) {
            effectsToApply.merge(registry.getKey(biome), mod, BiomeEffectModifier::merge);
        }
    }

    @Override
    public void apply() {
    }

    private void doApplyWithLevel(RegistryAccess registryAccess, boolean firstLogin) {
        if (!firstLogin && !needsDynamicApplication) return;

        needsDynamicApplication = false;
        if (firstLogin) vanillaEffects.clear();


        Registry<Biome> biomeReg = registryAccess.registry(Registries.BIOME).get();

        for (var v : effectsToApply.entrySet()) {

            ResourceLocation biomeId = v.getKey();
            BiomeEffectModifier modifier = v.getValue();
            var biome = biomeReg.getOptional(biomeId);
            if (biome.isPresent()) {
                var old = modifier.apply(biome.get());

                vanillaEffects.put(biomeId, old);

                if (modifier.modifyFogParameter()) {
                    fogParametersModifiers.put(biome.get(), modifier);
                }
            }
        }
        if (!vanillaEffects.isEmpty())
            Polytone.LOGGER.info("Applied {} Custom Biome Effects Properties", vanillaEffects.size());
        //we don't clear effects to apply because we need to re apply on world reload

    }

    @Override
    public void reset() {
        this.needsDynamicApplication = true;
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            Registry<Biome> biomeReg = level.registryAccess().registry(Registries.BIOME).get();
            for (var v : vanillaEffects.entrySet()) {
                var biome = biomeReg.getOptional(v.getKey());
                biome.ifPresent(bio -> BiomeEffectModifier.applyEffects(bio, v.getValue()));
            }
            //reset all
        }
        //if we don't have a level, biomes don't exist anymore, so we don't care

        vanillaEffects.clear();

        //whatever happens, we always clear stuff to apply
        effectsToApply.clear();

        fogParametersModifiers.clear();
    }


    private static float lastFogStart = 1;
    private static float lastFogEnd = 1;

    @Nullable
    public Vec2 modifyFogParameters(float originalNearPlane, float originalFarPlane) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return null;
        Level level = player.level();

        Holder<Biome> biome = level.getBiome(player.blockPosition());
        var fogMod = fogParametersModifiers.get(biome.value());
        Vec2 targetFog = null;
        if (fogMod != null) {
            targetFog = fogMod.modifyFogParameters(level);
        }

        if (targetFog == null && (Mth.abs(lastFogStart - 1) > 0.02f || Mth.abs(lastFogEnd - 1) > 0.02f)) {
            targetFog = new Vec2(1, 1);
        }
        if (targetFog != null) {
            float deltaTime = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(); // Get time since last frame
            float interpolationFactor = deltaTime * 0.1f;

            // Interpolate towards the targetFog values
            lastFogStart = Mth.lerp(interpolationFactor, lastFogStart, targetFog.x);
            lastFogEnd = Mth.lerp(interpolationFactor, lastFogEnd, targetFog.y);
            //fogEvent.scaleNearPlaneDistance(1);
            float distance = originalFarPlane - originalNearPlane;

            return new Vec2((originalFarPlane - distance * lastFogStart) * lastFogEnd, originalFarPlane * lastFogEnd);
        }

        return null;
    }
}
