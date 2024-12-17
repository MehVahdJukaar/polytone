package net.mehvahdjukaar.polytone.sound;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SoundTypesManager extends PartialReloader<SoundTypesManager.Resources> {

    private final MapRegistry<SoundEvent> customSoundEvents = new MapRegistry<>("Custom Sound Events");

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

        return new Resources(ImmutableMap.copyOf(jsons), ImmutableMap.copyOf(types));
    }

    @Override
    protected void parseWithLevel(Resources resources, RegistryOps<JsonElement> ops, RegistryAccess access) {

        var soundJsons = resources.soundTypes;
        var soundEvents = resources.soundEvents;

        //custom sound events

        for (var e : soundEvents.entrySet()) {
            for (var s : e.getValue()) {
                ResourceLocation id = e.getKey().withPath(s);
                if (!customSoundEvents.containsKey(id) && !BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
                    SoundEvent newSound = SoundEvent.createVariableRangeEvent(id);
                    customSoundEvents.register(id, newSound);
                } else {
                    Polytone.LOGGER.error("Sound Event with id {} already exists! Ignoring.", id);
                }
            }
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
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        for (var e : customSoundEvents.getEntries()) {
            var id = e.getKey();
            var sound = e.getValue();
            PlatStuff.registerDynamic(BuiltInRegistries.SOUND_EVENT, id, sound);
        }

        if (!customSoundEvents.isEmpty()) {
            Polytone.LOGGER.info("Registered {} custom Sound Events from Resource Packs: {}", customSoundEvents.size(), customSoundEvents + ". Remember to add them to sounds.json!");
            //this is bad
            Minecraft.getInstance().getSoundManager().reload();
            //this entire thing is a bad idea
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        for(var e : customSoundEvents.getEntries()) {
            var id  = e.getKey();
            PlatStuff.unregisterDynamic(BuiltInRegistries.SOUND_EVENT, id);
        }
        customSoundTypes.clear();
        customSoundEvents.clear();
    }

    public record Resources(Map<ResourceLocation, JsonElement> soundTypes,
                            Map<ResourceLocation, List<String>> soundEvents) {
    }

}
