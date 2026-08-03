package net.mehvahdjukaar.polytone.content.colormap;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.companion.TexturePart;
import net.mehvahdjukaar.polytone.common.companion.TrackedTextures;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.DryFoliageColor;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class ColormapsManager extends ContentManager<Colormap> {

    // Builtin colormaps
    //TODO: delegate to grass so we have quark compat
    public static final IColorGetter GRASS_COLOR = new IColorGetter.OfColorResolver(
            new BlockTintSource() {
                @Override public int color(BlockState s) { return GrassColor.getDefaultColor(); }
                @Override public int colorInWorld(BlockState s, BlockAndTintGetter l, BlockPos p) { return BiomeColors.getAverageGrassColor(l, p); }
            },
            BiomeColors.GRASS_COLOR_RESOLVER);

    public static final IColorGetter FOLIAGE_COLOR = new IColorGetter.OfColorResolver(
            new BlockTintSource() {
                @Override public int color(BlockState s) { return FoliageColor.get(0.5, 1.0); }
                @Override public int colorInWorld(BlockState s, BlockAndTintGetter l, BlockPos p) { return BiomeColors.getAverageFoliageColor(l, p); }
            },
            BiomeColors.FOLIAGE_COLOR_RESOLVER);

    public static final IColorGetter DRY_FOLIAGE_COLOR = new IColorGetter.OfColorResolver(
            new BlockTintSource() {
                @Override public int color(BlockState s) { return DryFoliageColor.get(0.5, 1.0); }
                @Override public int colorInWorld(BlockState s, BlockAndTintGetter l, BlockPos p) { return BiomeColors.getAverageDryFoliageColor(l, p); }
            },
            BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER);

    public static final IColorGetter WATER_COLOR = new IColorGetter.OfColorResolver(
            new BlockTintSource() {
                @Override public int color(BlockState s) { return 0xFF000000; }
                @Override public int colorInWorld(BlockState s, BlockAndTintGetter l, BlockPos p) { return BiomeColors.getAverageWaterColor(l, p); }
            },
            BiomeColors.WATER_COLOR_RESOLVER);

    // custom defined colormaps
    private final MapRegistry<Supplier<IColorGetter>> colormaps = new MapRegistry<>("Polytone Colormaps");
    private final Map<IColorGetter, IColorGetter> concurrentColormaps = new HashMap<>();


    public Codec<IColorGetter> byNameCodec() {
        return colormaps.xmap(Supplier::get, s -> () -> s);
    }

    //dumb but better than codec madness since we have the supplier thing here
    public IColorGetter getOrCreateConcurrentColormap(IColorGetter colormap) {
        return concurrentColormaps.computeIfAbsent(colormap, IColorGetter::makeConcurrent);
    }

    private static final TexturePart<Colormap> TEXTURE = TexturePart.plain(c -> c);

    public ColormapsManager() {
        super(Spec.of("Colormap", () -> Colormap.DIRECT_CODEC)
                .wikiPage("Colormaps")
                .textureParts(TEXTURE)
                .folders("colormaps"));
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        //builtin stuff
        colormaps.register(Identifier.parse("grass_color"), () -> GRASS_COLOR);
        colormaps.register(Identifier.parse("foliage_color"), () -> FOLIAGE_COLOR);
        colormaps.register(Identifier.parse("dry_foliage_color"), () -> DRY_FOLIAGE_COLOR);
        colormaps.register(Identifier.parse("water_color"), () -> WATER_COLOR);
        //These create new incomplete ones every time
        colormaps.register(Identifier.parse("biome_sample"), Colormap::createDefSquare);
        colormaps.register(Identifier.parse("triangular_biome_sample"), Colormap::createDefTriangle);
        colormaps.register(Identifier.parse("fixed"), Colormap::createFixed);
        colormaps.register(Identifier.parse("grid"), Colormap::createBiomeId);
        colormaps.register(Identifier.parse("damage"), Colormap::createDamage);

        var jsons = resources.jsons();
        var textures = new TrackedTextures(resources.textures());

        for (var j : jsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            Colormap colormap = decodeStrict(json, id, ops);
            colormap.inlined = false;
            // the contract declared on the Spec enumerates the bound slot,
            // and fill() resolves it against the scanned textures.
            contentTexture.fill(textures, id, colormap, true);

            // we need to fill these before we parse the properties as they will be referenced below
            add(id, colormap);
        }

        //initialize recursive stuff
        /*
        for (var c : colormaps.getValues()) {
            if (c.get() instanceof Colormap cm && cm.lazyFallback != null) {
                try {
                    cm.fallback = runCodec(ops, cm.lazyFallback);
                } catch (Exception e) {
                    Polytone.LOGGER.error("Failed to initialize colormap fallback", e);
                }
                cm.lazyFallback = null;
            }
        }*/


        // creates orphaned texture colormaps
        for (var orphan : contentTexture.orphans(textures, jsons.keySet())) {
            Colormap defaultColormap = Colormap.createDefTriangle();
            defaultColormap.inlined = false;
            contentTexture.fill(textures, orphan.stemId(), defaultColormap, true);
            // we need to fill these before we parse the properties as they will be referenced below
            add(orphan.stemId(), defaultColormap);
        }
    }

    private <T> IColorGetter runCodec(DynamicOps o, Dynamic<T> dynamic) {
        DynamicOps<T> ops = (DynamicOps<T>) o;
        return this.byNameCodec().decode(ops, dynamic.getValue())
                .getOrThrow().getFirst();
    }


    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        colormaps.clear();
        concurrentColormaps.clear();
        PlatStuff.unregisterAllCustomColorResolves();
    }

    public void add(Identifier id, Colormap colormap) {
        colormaps.register(id, () -> colormap);
        if (colormap.needsToFillTexture()) {
            throw new IllegalStateException("Did not find any texture png for colormap " + id);
        }
    }


    public Collection<Identifier> getAllNames() {
        return colormaps.keySet();
    }

    @Nullable
    public IColorGetter get(String id) {
        var c = colormaps.getValue(id);
        return c != null ? c.get() : null;
    }
}
