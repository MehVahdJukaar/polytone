package net.mehvahdjukaar.polytone.content.sound;

import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.CsvUtils;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.polytone.common.Parsed;
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

public class SoundTypesManager extends ContentManager<PolytoneSoundType> {

    private Map<ResourceLocation, List<String>> soundEvents = Map.of();

    private final MapRegistry<SoundEvent> customSoundEvents = new MapRegistry<>("Custom Sound Events");

    private final MapRegistry<SoundType> customSoundTypes = new MapRegistry<>("Custom Sound Types");

    public SoundTypesManager() {
        super(Spec.of("Sound type", () -> PolytoneSoundType.DIRECT_CODEC)
                .wikiPage("Custom-Sound-Events")
                .folders("custom_sound_types", "sound_types"));
    }

    @Nullable
    public SoundType getCustomSoundType(ResourceLocation resourceLocation) {
        return customSoundTypes.getValue(resourceLocation);
    }

    @Override
    protected AssetsFiles prepare(ResourceManager resourceManager) {
        this.soundEvents = ImmutableMap.copyOf(CsvUtils.parseCsv(resourceManager, "sound_events"));
        return super.prepare(resourceManager);
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {

        //custom sound events

        for (var e : this.soundEvents.entrySet()) {
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

        for (var e : customSoundEvents.getEntries()) {
            var id = e.getKey();
            var sound = e.getValue();
            PlatStuff.registerDynamic(BuiltInRegistries.SOUND_EVENT, id, sound);
        }

        // sound types

        for (var j : Parsed.batchParseOnlyEnabled(resources.jsons(), PolytoneSoundType.DIRECT_CODEC,
                ops, "sound type")) {
            var soundType = j.getValue();
            var id = j.getKey();
            customSoundTypes.register(id, soundType);
        }
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {

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
            ResourceLocation id  = e.getKey();
            PlatStuff.unregisterDynamic(BuiltInRegistries.SOUND_EVENT, id);
        }
        customSoundTypes.clear();
        customSoundEvents.clear();
    }

    public boolean isDynamicSound(ResourceLocation entryId) {
        return customSoundEvents.containsKey(entryId);
    }

}
