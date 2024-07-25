package net.mehvahdjukaar.polytone.texture;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class VariantTextureManager extends JsonPartialReloader {

    private final WeakHashMap<BakedQuad, Map<ResourceLocation, BakedQuad>> variantQuadsCache = new WeakHashMap<>();

    private final Map<Block, VariantTexture> blocksWithVariants = new Object2ObjectOpenHashMap<>();

    // list of blocks that will have their tint sent to 0.
    // why? because optifine is crap and allows assigning colors to models without a tint index.
    // Just edit your models people!
    private final Set<Block> forceTintBlocks = new HashSet<>();

    public VariantTextureManager() {
        super("variant_textures");
    }

    @Override
    public void process(Map<ResourceLocation, JsonElement> jsonElementMap, DynamicOps<JsonElement> ops) {

        for (var j : jsonElementMap.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            VariantTexture variant = VariantTexture.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Variant Texture with json id " + id + "\n error: " + errorMsg)).getFirst();
            addVariant(id, variant);
        }
    }

    private void addVariant(ResourceLocation pathId, VariantTexture mod) {
        for (Block b : mod.getTargets(pathId, BuiltInRegistries.BLOCK)) {
            var old = blocksWithVariants.put(b, mod);
            if (old != null) {
                Polytone.LOGGER.warn("Found 2 Variant Textures jsons with same target ({}). Overriding", b);
            }
        }
    }

    @Override
    protected void reset() {
        blocksWithVariants.clear();
        variantQuadsCache.clear(); //we might need a lock here
        forceTintBlocks.clear();
    }


    public boolean shouldSetTintTo0(int tintIndex, BlockAndTintGetter blockView, BlockState state, BlockPos blockPos) {
        if (tintIndex == -1 && !forceTintBlocks.isEmpty() && state != null) {
            return forceTintBlocks.contains(state.getBlock());
        }
        return false;
    }

    public BakedQuad maybeModify(BakedQuad quad, BlockAndTintGetter level, BlockState state, BlockPos pos) {
        if (quad.tintIndex == -1 && !forceTintBlocks.isEmpty()) {
            if (forceTintBlocks.contains(state.getBlock())) quad.tintIndex = 0;
        }
        if (blocksWithVariants.isEmpty()) return null;
        Block block = state.getBlock();
        var variant = blocksWithVariants.get(block);
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

    public void addTintOverrideHack(Block block) {
        if (block != Blocks.GRASS_BLOCK) {//hardcoding yay
            forceTintBlocks.add(block);
        }
    }


}
