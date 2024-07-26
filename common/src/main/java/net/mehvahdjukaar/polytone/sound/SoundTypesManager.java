package net.mehvahdjukaar.polytone.sound;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SoundTypesManager extends PartialReloader<SoundTypesManager.Resources> {

    private final Set<ResourceLocation> customSoundEvents = new HashSet<>();

    // custom defined sound types
    private final MapRegistry<SoundType> customSoundTypes = new MapRegistry<>("Custom Sound Types");

    public SoundTypesManager() {
        super("custom_sound_types", "sound_types");
    }

    @Nullable
    public SoundType getCustomSoundType(ResourceLocation resourceLocation) {
        return customSoundTypes.getValue(resourceLocation);
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        var jsons = getJsonsInDirectories(resourceManager);
        this.checkConditions(jsons);

        var types = CsvUtils.parseCsv(resourceManager, "sound_events");

        return new Resources(jsons, types);
    }

    @Override
    public void process(Resources resources, DynamicOps<JsonElement> ops) {

        var soundJsons = resources.soundTypes;
        var soundEvents = resources.soundEvents;

        //custom sound events

        for (var e : soundEvents.entrySet()) {
            for (var s : e.getValue()) {
                ResourceLocation id = e.getKey().withPath(s);
                if (!customSoundEvents.contains(id) && !BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
                    SoundEvent newSound = PlatStuff.registerSoundEvent(id);
                    customSoundEvents.add(id);
                } else {
                    Polytone.LOGGER.error("Sound Event with id {} already exists! Ignoring.", id);
                }
            }
        }

        if (!customSoundEvents.isEmpty()) {
            Polytone.LOGGER.info("Registered {} custom Sound Events from Resource Packs: {}", customSoundEvents.size(), customSoundEvents + ". Remember to add them to sounds.json!");
            //this is bad
            Minecraft.getInstance().getSoundManager().reload();
            //this entire thing is a bad idea
        }

        // sound types

        for (var j : soundJsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            SoundType soundType = PolytoneSoundType.DIRECT_CODEC.decode(ops, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Sound Type with json id {} - error: {}",
                            id, errorMsg)).getFirst();

            customSoundTypes.register(id, soundType);
        }
    }

    @Override
    protected void reset() {
        customSoundTypes.clear();
        customSoundEvents.clear();
    }




    public record Resources(Map<ResourceLocation, JsonElement> soundTypes,
                            Map<ResourceLocation, List<String>> soundEvents) {
    }

}
