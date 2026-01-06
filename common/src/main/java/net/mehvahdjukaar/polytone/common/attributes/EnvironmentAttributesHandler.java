package net.mehvahdjukaar.polytone.common.attributes;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;

import java.util.WeakHashMap;

public class EnvironmentAttributesHandler {

    public static final WeakHashMap<ClientLevel, EnvironmentAttributeSystem> vanillaSystem = new WeakHashMap<>();

    private static long lastRefreshedTimestamp;

    public static void refresh() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        //Debounce since we can call twice. for ease of use due to dimension changing happening too late
        long thisTimestamp = level.getGameTime();
        if (thisTimestamp == lastRefreshedTimestamp) {
            return;
        }
        lastRefreshedTimestamp = thisTimestamp;
        if (Polytone.DIMENSION_MODIFIERS.hasModifiedAttributes() || Polytone.BIOME_MODIFIERS.hasModifiedAttributes() ||
                Polytone.COLORS.getSkyFlash() != null) {
            EnvironmentAttributeSystem old = level.environmentAttributes;
            if (!vanillaSystem.containsKey(level)) {
                vanillaSystem.put(level, old);
            }
            //same as vanilla does. if other mods add stuff here this might break them...
            level.environmentAttributes = level.addEnvironmentAttributeLayers(EnvironmentAttributeSystem.builder()).build();
        }
    }

    public static void reset() {
        for (var entry : vanillaSystem.entrySet()) {
            ClientLevel level = entry.getKey();
            level.environmentAttributes = entry.getValue();
        }
        vanillaSystem.clear();
    }
}
