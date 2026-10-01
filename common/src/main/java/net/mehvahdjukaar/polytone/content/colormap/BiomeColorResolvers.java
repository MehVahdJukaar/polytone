package net.mehvahdjukaar.polytone.content.colormap;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.ColorResolver;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public enum BiomeColorResolvers {
    GRASS("grass", () -> BiomeColors.GRASS_COLOR_RESOLVER, r -> BiomeColors.GRASS_COLOR_RESOLVER = r),
    FOLIAGE("foliage", () -> BiomeColors.FOLIAGE_COLOR_RESOLVER, r -> BiomeColors.FOLIAGE_COLOR_RESOLVER = r),
    DRY_FOLIAGE("dry_foliage", () -> BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER, r -> BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER = r),
    WATER("water", () -> BiomeColors.WATER_COLOR_RESOLVER, r -> BiomeColors.WATER_COLOR_RESOLVER = r);

    private final String key;
    private final Supplier<ColorResolver> getter;
    private final Consumer<ColorResolver> setter;
    @Nullable
    private ColorResolver vanilla;
    private boolean setFromColorsJson;

    BiomeColorResolvers(String key, Supplier<ColorResolver> getter, Consumer<ColorResolver> setter) {
        this.key = key;
        this.getter = getter;
        this.setter = setter;
    }

    @Nullable
    public static BiomeColorResolvers byKey(String key) {
        for (var r : values()) {
            if (r.key.equals(key)) return r;
        }
        return null;
    }

    public boolean isReplaced() {
        return vanilla != null;
    }

    public boolean replaceExplicitly(IColorGetter colormap) {
        ColorResolver resolver = asResolver(colormap);
        if (resolver == null) return false;
        swap(resolver);
        setFromColorsJson = true;
        return true;
    }

    public void replaceImplicitly(IColorGetter colormap) {
        if (setFromColorsJson) return;
        ColorResolver resolver = asResolver(colormap);
        if (resolver != null) swap(resolver);
    }

    private void swap(ColorResolver resolver) {
        if (vanilla == null) vanilla = getter.get();
        setter.accept(resolver);
    }

    public static void resetAll() {
        for (var r : values()) {
            if (r.vanilla != null) r.setter.accept(r.vanilla);
            r.vanilla = null;
            r.setFromColorsJson = false;
        }
    }

    @Nullable
    private static ColorResolver asResolver(IColorGetter color) {
        if (color instanceof ColorResolver c) return c;
        if (color instanceof IndexCompoundColorGetter ic) {
            for (var g : ic.getGetters().values()) {
                if (g instanceof ColorResolver c) return c;
            }
        }
        return null;
    }
}
