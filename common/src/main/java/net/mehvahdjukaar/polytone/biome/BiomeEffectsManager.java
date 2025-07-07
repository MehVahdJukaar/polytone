package net.mehvahdjukaar.polytone.biome;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.block.BlockPropertyModifier;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.Parsed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.material.Fluids;
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

    private final Map<Biome, BiomeEffectModifier> fogParametersModifiers = new HashMap<>();

    @Override
    public void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, RegistryAccess access) {
        for (var v : Parsed.batchParseOnlyEnabled(jsons, BiomeEffectModifier.CODEC, ops, "biome modifier")) {
            addEffect(v.getKey(), v.getValue(), access);
        }
    }

    private void addEffect(ResourceLocation pathId, BiomeEffectModifier mod, RegistryAccess access) {
        Registry<Biome> registry = access.registryOrThrow(Registries.BIOME);
        for (var biome : mod.targets().compute(pathId, registry)) {
            effectsToApply.merge(biome.unwrapKey().get().location(), mod, BiomeEffectModifier::merge);
        }
    }

    // we need registry ops here since special effects use registry stuff...
    @Override
    public void applyWithLevel(RegistryAccess registryAccess, boolean isLogIn) {
        if (!isLogIn && !needsDynamicApplication) return;

        needsDynamicApplication = false;
        if (isLogIn) vanillaEffects.clear();


        Registry<Biome> biomeReg = registryAccess.registry(Registries.BIOME).get();
        //addAllWaterColors(biomeReg, registryAccess);
        //pribably needed for sodium hack

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
    public void resetWithLevel(boolean isLogOff) {
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


    public BiomeEffectModifier getFogModifier(Biome value) {
        return fogParametersModifiers.get(value);
    }

}
