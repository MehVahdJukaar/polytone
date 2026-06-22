package net.mehvahdjukaar.polytone;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

//can be early loaded for
public class PolytoneStub {

    static boolean initialized = false;

    public static boolean isEntryDynamic(Registry<?> reg, ResourceLocation entryId) {
        if (!initialized) return false;
        if (reg == BuiltInRegistries.CREATIVE_MODE_TAB) {
            return Polytone.CREATIVE_TABS_MODIFIERS.isDynamicTab(entryId);
        }
        if (reg == BuiltInRegistries.PARTICLE_TYPE) {
            return Polytone.CUSTOM_PARTICLES.isDynamicParticle(entryId);
        }
        if (reg == BuiltInRegistries.SOUND_EVENT) {
            return Polytone.SOUND_TYPES.isDynamicSound(entryId);
        }
        return false;
    }
}
