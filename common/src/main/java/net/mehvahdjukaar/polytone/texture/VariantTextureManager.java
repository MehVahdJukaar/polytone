package net.mehvahdjukaar.polytone.texture;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.BakedQuadsTransformer;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

public class VariantTextureManager extends JsonPartialReloader {

    private final WeakHashMap<BakedQuad, Map<ResourceLocation, BakedQuad>> variantQuadsCache = new WeakHashMap();

    private final Map<Block, VariantTexture> blocksWithVariants = new Object2ObjectOpenHashMap<>();

    public VariantTextureManager(){
        super("variant_textures");
    }

    @Override
    public void process(Map<ResourceLocation, JsonElement> jsonElementMap) {

        for (var j : jsonElementMap.entrySet()) {
            var json = j.getValue();
            var res = j.getKey();
            VariantTexture variant = VariantTexture.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Variant Texture with json res {} - error: {}",
                            res, errorMsg)).getFirst();
            var target = Polytone.getTarget(res, BuiltInRegistries.BLOCK);
            if (target != null) {
                blocksWithVariants.put(target.getFirst(), variant);
            }
        }
    }

    @Override
    protected void reset() {
        blocksWithVariants.clear();
        variantQuadsCache.clear(); //we might need a lock here
    }

    public BakedQuad maybeModify(BakedQuad quad, BlockAndTintGetter level, BlockState state, BlockPos pos) {
        if (blocksWithVariants.isEmpty()) return null;
        var variant = blocksWithVariants.get(state.getBlock());
        if (variant != null) {
            var biomeToTexture = variant.getBiomeMap(quad.getSprite());
            if (biomeToTexture != null && level instanceof RenderChunkRegion region) {
                Holder<Biome> biome = region.level.getBiome(pos);
                ResourceLocation biomeLoc = biome.unwrapKey().get().location();
                ResourceLocation newTexture = biomeToTexture.get(biomeLoc);
                if (newTexture != null) {
                    return getOrCreateQuad(quad, biomeLoc, newTexture);
                }
            }
        }
        return null;
    }

    @NotNull
    private BakedQuad getOrCreateQuad(BakedQuad quad, ResourceLocation biome, ResourceLocation newTexture) {
        return variantQuadsCache.computeIfAbsent(quad, q -> new WeakHashMap<>())
                .computeIfAbsent(biome,
                        b -> {
                            var s = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(newTexture);
                            BakedQuadsTransformer transformer = BakedQuadsTransformer.create()
                                    .applyingSprite(s);

                            return transformer.transform(quad);

                        });
    }

}
