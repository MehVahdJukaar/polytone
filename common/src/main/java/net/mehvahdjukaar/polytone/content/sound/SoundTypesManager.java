package net.mehvahdjukaar.polytone.content.sound;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.struc.CsvUtils;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class SoundTypesManager extends ContentManager<PolytoneSoundType> {

    // sound_events.csv sidecar — new sound-event ids to register, keyed by pack namespace
    private Map<Identifier, List<String>> soundEvents = Map.of();

    private final MapRegistry<SoundEvent> customSoundEvents = new MapRegistry<>("Custom Sound Events");

    // custom defined sound types
    private final MapRegistry<SoundType> customSoundTypes = new MapRegistry<>("Custom Sound Types");

    public SoundTypesManager() {
        super(Spec.of("Sound type", () -> PolytoneSoundType.DIRECT_CODEC)
                .wikiPage("Custom-Sound-Events")
                .folders("custom_sound_types", "sound_types"));
    }

    @Nullable
    public SoundType getCustomSoundType(Identifier resourceLocation) {
        return customSoundTypes.getValue(resourceLocation);
    }

    @Override
    protected AssetsFiles prepare(PreparableReloadListener.SharedState sharedState) {
        var resourceManager = sharedState.resourceManager();
        this.soundEvents = ImmutableMap.copyOf(CsvUtils.parseCsv(resourceManager, "sound_events"));
        return super.prepare(sharedState);
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {

        //custom sound events
        for (var e : this.soundEvents.entrySet()) {
            for (var s : e.getValue()) {
                Identifier id = e.getKey().withPath(s);
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

        for (var j : parseEnabledJsons(resources.jsons(), ops)) {
            var soundType = j.getValue();
            var id = j.getKey();
            customSoundTypes.register(id, soundType);
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        if (!customSoundEvents.isEmpty()) {
            Polytone.LOGGER.info("Registered {} custom Sound Events", customSoundEvents.size()) ;
            //this is bad
            Minecraft.getInstance().getSoundManager().reload();
            //this entire thing is a bad idea
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        for(var e : customSoundEvents.getEntries()) {
            Identifier id  = e.getKey();
            PlatStuff.unregisterDynamic(BuiltInRegistries.SOUND_EVENT, id);
        }
        customSoundTypes.clear();
        customSoundEvents.clear();
    }

    public boolean isDynamicSound(Identifier entryId) {
        return customSoundEvents.containsKey(entryId);
    }


}
