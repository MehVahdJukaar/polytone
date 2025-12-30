package net.mehvahdjukaar.polytone.common.attributes;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;

import java.util.WeakHashMap;

public class EnvironmentAttributesHandler {

    public static final WeakHashMap<ClientLevel, EnvironmentAttributeSystem> vanillaSystem = new WeakHashMap<>();

    public static void refresh() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (Polytone.DIMENSION_MODIFIERS.hasModifiedAttributes() || Polytone.BIOME_MODIFIERS.hasModifiedAttributes()) {
            EnvironmentAttributeSystem old = level.environmentAttributes;
            if (!vanillaSystem.containsKey(level)) {
                vanillaSystem.put(level, old);
            }
            //same as vanilla does. if other mods add stuff here this might break them...
            level.environmentAttributes = EnvironmentAttributeSystem.builder()
                    .addDefaultLayers(level).build();
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
