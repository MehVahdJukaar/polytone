package net.mehvahdjukaar.polytone.common.attributes;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;

import java.util.WeakHashMap;

public class EnvironmentAttributesSystemRebuilder {

    //backup of vanilla system
    private static final WeakHashMap<ClientLevel, EnvironmentAttributeSystem> vanillaSystemByLevel = new WeakHashMap<>();

    private static long lastRefreshGameTime;

    public static void refresh() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        //Debounce since we can call twice. for ease of use due to dimension changing happening too late
        long gameTime = level.getGameTime();
        if (gameTime == lastRefreshGameTime) return;
        lastRefreshGameTime = gameTime;

        if (!anyPackModifiesAttributes()) return;

        vanillaSystemByLevel.putIfAbsent(level, level.environmentAttributes);
        //the builder re latches this if any dynamic layer makes it into the new system
        DynamicAttributeContext.hasDynamicLayers = false;
        //same as vanilla does. if other mods add stuff here this might break them...
        //mixin builds system from here
        level.environmentAttributes = level.addEnvironmentAttributeLayers(EnvironmentAttributeSystem.builder()).build();
    }

    private static boolean anyPackModifiesAttributes() {
        return Polytone.DIMENSION_MODIFIERS.hasModifiedAttributes()
                || Polytone.BIOME_MODIFIERS.hasModifiedAttributes()
                || Polytone.COLORS.getSkyFlash() != null;
    }

    public static void reset() {
        vanillaSystemByLevel.forEach((level, vanillaSystem) -> level.environmentAttributes = vanillaSystem);
        vanillaSystemByLevel.clear();
        DynamicAttributeContext.hasDynamicLayers = false;
    }
}
