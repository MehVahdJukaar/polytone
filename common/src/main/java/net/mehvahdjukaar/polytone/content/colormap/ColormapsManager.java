package net.mehvahdjukaar.polytone.content.colormap;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.companion.TextureRole;
import net.mehvahdjukaar.polytone.common.companion.ScannedTextures;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ColormapsManager extends ContentManager<IColorGetter> {

    // Builtin colormaps
    //TODO: delegate to grass so we have quark compat
    public static final IColorGetter GRASS_COLOR = new IColorGetter.ofColorResolver((s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageGrassColor(l, p) : GrassColor.getDefaultColor(),
            BiomeColors.GRASS_COLOR_RESOLVER);

    public static final IColorGetter FOLIAGE_COLOR = new IColorGetter.ofColorResolver((s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageFoliageColor(l, p) : FoliageColor.get(0.5, 1.0),
            BiomeColors.FOLIAGE_COLOR_RESOLVER);

    public static final IColorGetter WATER_COLOR = new IColorGetter.ofColorResolver((s, l, p, i) ->
            l != null && p != null ? BiomeColors.getAverageWaterColor(l, p) : 0xFF000000,
            BiomeColors.WATER_COLOR_RESOLVER);

    // custom defined colormaps
    private final MapRegistry<Supplier<IColorGetter>> colormaps = new MapRegistry<>("Polytone Colormaps");
    private final Map<IColorGetter, IColorGetter> concurrentColormaps = new HashMap<>();


    public Codec<IColorGetter> byNameCodec() {
        return colormaps.xmap(Supplier::get, s -> () -> s);
    }

    @Nullable
    public IColorGetter get(String name) {
        var s = colormaps.getValue(name);
        return s == null ? null : s.get();
    }

    //dumb but better than codec madness since we have the supplier thing here
    public IColorGetter getOrCreateConcurrentColormap(IColorGetter colormap) {
        return concurrentColormaps.computeIfAbsent(colormap, IColorGetter::makeConcurrent);
    }

    // plain naming still gives indexed compounds <stem>_<n>.png for their inline members
    private static final TextureRole<IColorGetter> TEXTURE = TextureRole.plain(c -> c);

    public ColormapsManager() {
        super(Spec.of("Colormap", () -> SchemaCodecs.<IColorGetter>alternatives(
                        SchemaCodecs.alt("inline colormap", Colormap.DIRECT_CODEC),
                        SchemaCodecs.alt("biome compound", BiomeCompoundColorGetter.CODEC),
                        SchemaCodecs.alt("indexed compound", IndexCompoundColorGetter.DIRECT_CODEC),
                        SchemaCodecs.alt("reference", Polytone.COLORMAPS.byNameCodec()),
                        SchemaCodecs.alt("single color", Colormap.SINGLE_COLOR_CODEC)))
                .wikiPage("Colormaps")
                .textureParts(TEXTURE)
                .folders("colormaps"));
    }


    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        addBuiltinColormaps();

        var jsons = resources.jsons();
        var textures = new ScannedTextures(resources.textures());

        // compounds and aliases point at other files here, so keep retrying until nothing new decodes
        Map<ResourceLocation, JsonElement> pending = new LinkedHashMap<>(jsons);
        boolean progressed = true;
        while (!pending.isEmpty() && progressed) {
            progressed = false;
            var it = pending.entrySet().iterator();
            while (it.hasNext()) {
                var j = it.next();
                IColorGetter colormap = contentCodec().decode(ops, j.getValue()).result()
                        .map(Pair::getFirst).orElse(null);
                if (colormap == null) continue;
                contentTexture.fill(textures, j.getKey(), colormap, true);
                add(j.getKey(), colormap);
                it.remove();
                progressed = true;
            }
        }
        // whatever is left is broken or cyclic. decode again just for the error
        for (var j : pending.entrySet()) {
            decodeStrict(j.getValue(), j.getKey(), ops);
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
            contentTexture.fill(textures, orphan.stemId(), defaultColormap, true);
            add(orphan.stemId(), defaultColormap);
        }
    }

    private <T> IColorGetter runCodec(DynamicOps o, Dynamic<T> dynamic) {
        DynamicOps<T> ops = (DynamicOps<T>) o;
        return this.byNameCodec().decode(ops, dynamic.getValue())
                .getOrThrow().getFirst();
    }


    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }

    private void addBuiltinColormaps() {
        colormaps.register(ResourceLocation.parse("grass_color"), () -> GRASS_COLOR);
        colormaps.register(ResourceLocation.parse("foliage_color"), () -> FOLIAGE_COLOR);
        colormaps.register(ResourceLocation.parse("water_color"), () -> WATER_COLOR);
        //These create new incomplete ones every time
        colormaps.register(ResourceLocation.parse("biome_sample"), Colormap::createDefSquare);
        colormaps.register(ResourceLocation.parse("triangular_biome_sample"), Colormap::createDefTriangle);
        colormaps.register(ResourceLocation.parse("fixed"), Colormap::createFixed);
        colormaps.register(ResourceLocation.parse("grid"), Colormap::createBiomeId);
        colormaps.register(ResourceLocation.parse("damage"), Colormap::createDamage);
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        colormaps.clear();
        concurrentColormaps.clear();
        PlatStuff.unregisterAllCustomColorResolves();
    }

    public void add(ResourceLocation id, IColorGetter colormap) {
        if (colormap instanceof Colormap c) c.inlined = false;
        colormaps.register(id, () -> colormap);
        if (colormap.needsToFillTexture()) {
            throw new IllegalStateException("Did not find any texture png for colormap " + id);
        }
    }


}
