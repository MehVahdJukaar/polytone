package net.mehvahdjukaar.polytone.biome;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BiomeEffectsManager extends JsonPartialReloader {

    private final Map<ResourceKey<Biome>, BiomeSpecialEffects> vanillaEffects = new HashMap<>();

    private final Map<ResourceKey<Biome>, BiomeEffectModifier> effectsToApply = new HashMap<>();
    private boolean needsDynamicApplication = true;

    public BiomeEffectsManager() {
        super("biome_modifiers", "biome_effects");
    }

    private final Map<Biome, BiomeEffectModifier> fogParametersModifiers = new HashMap<>();

    @Override
    public void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            BiomeEffectModifier effect = BiomeEffectModifier.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Biome Special Effect with json id " + id + "\n error: " + errorMsg))
                    .getFirst();

            addEffect(id, effect, access);
        }
    }

    private void addEffect(ResourceLocation pathId, BiomeEffectModifier mod, HolderLookup.Provider access) {
        var registry = access.lookupOrThrow(Registries.BIOME);
        for (var biome : mod.targets().compute(pathId, registry)) {
            effectsToApply.merge(biome.unwrapKey().get(), mod, BiomeEffectModifier::merge);
        }
    }

    // we need registry ops here since special effects use registry stuff...
    @Override
    public void applyWithLevel(HolderLookup.Provider registryAccess, boolean isLogIn) {
        if (!isLogIn && !needsDynamicApplication) return;

        needsDynamicApplication = false;
        if (isLogIn) vanillaEffects.clear();


        var biomeReg = registryAccess.lookupOrThrow(Registries.BIOME);

        for (var v : effectsToApply.entrySet()) {

            var biomeId = v.getKey();
            BiomeEffectModifier modifier = v.getValue();
            var biome = biomeReg.get(biomeId);
            if (biome.isPresent()) {
                var old = modifier.apply(biome.get().value());

                vanillaEffects.put(biomeId, old);

                if (modifier.modifyFogParameter()) {
                    fogParametersModifiers.put(biome.get().value(), modifier);
                }
            }
        }
        if (!vanillaEffects.isEmpty())
            Polytone.LOGGER.info("Applied {} Custom Biome Effects Properties", vanillaEffects.size());
        //we don't clear effects to apply because we need to re apply on world reload

    }

    @Override
    public void resetWithLevel(boolean isLogOff) {
        this.needsDynamicApplication = true;
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            Registry<Biome> biomeReg = level.registryAccess().lookupOrThrow(Registries.BIOME);
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


    private static float lastFogDistanceMult = 1;
    private static float lastFogEndMult = 1;

    @Nullable
    public Vec2 modifyFogParameters(float originalNearPlane, float originalFarPlane) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return null;

        //dont modify if a mob effect that modifies fog is active
        if (FogRenderer.getPriorityFogFunction(player, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false))
                != null) return null;

        Level level = player.level();

        Holder<Biome> biome = level.getBiome(player.blockPosition());
        var fogMod = fogParametersModifiers.get(biome.value());
        Vec2 fogScalars = null;
        if (fogMod != null) {
            fogScalars = fogMod.modifyFogParameters(level);
        }

        if (fogScalars == null && (Mth.abs(lastFogDistanceMult - 1) > 0.02f || Mth.abs(lastFogEndMult - 1) > 0.02f)) {
            fogScalars = new Vec2(1, 1);
        }
        if (fogScalars != null) {
            float deltaTime = ClientFrameTicker.getDeltaTime(); // Get time since last frame
            float interpolationFactor = deltaTime * 0.1f;

            // Interpolate towards the fogScalars values
            lastFogDistanceMult = Mth.lerp(interpolationFactor, lastFogDistanceMult, fogScalars.x);
            lastFogEndMult = Mth.lerp(interpolationFactor, lastFogEndMult, fogScalars.y);
            //fogEvent.scaleNearPlaneDistance(1);
            float distance = originalFarPlane - originalNearPlane;

            return new Vec2((originalFarPlane - distance * lastFogDistanceMult) * lastFogEndMult, originalFarPlane * lastFogEndMult);
        }

        return null;
    }
}
