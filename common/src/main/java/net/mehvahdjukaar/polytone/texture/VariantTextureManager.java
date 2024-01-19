package net.mehvahdjukaar.polytone.texture;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.particle.ParticleModifier;
import net.mehvahdjukaar.polytone.utils.BakedQuadsTransformer;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class VariantTextureManager extends JsonPartialReloader {

    private final WeakHashMap<BakedQuad, Map<ResourceLocation, BakedQuad>> variantQuadsCache = new WeakHashMap();

    private final Map<Block, VariantTexture> blocksWithVariants = new Object2ObjectOpenHashMap<>();
    public final Set<Block> specialOFTintHack = new HashSet<>();

    public VariantTextureManager() {
        super("variant_textures");
    }

    @Override
    public void process(Map<ResourceLocation, JsonElement> jsonElementMap) {

        for (var j : jsonElementMap.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            VariantTexture variant = VariantTexture.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Variant Texture with json res {} - error: {}",
                            id, errorMsg)).getFirst();
            addVariant(id, variant);
        }
    }

    private void addVariant(ResourceLocation pathId, VariantTexture mod) {
        var explTargets = mod.explicitTargets();
        var pathTarget = Registry.BLOCK.getOptional(pathId);
        if (explTargets.isPresent()) {
            if (pathTarget.isPresent()) {
                Polytone.LOGGER.error("Found Variant Texture with Explicit Targets ({}) also having a valid IMPLICIT Path Target ({})." +
                        "Consider moving it under your OWN namespace to avoid overriding other packs modifiers with the same path", explTargets.get(), pathId);
            }
            for (var explicitId : explTargets.get()) {
                var target = Registry.BLOCK.getOptional(explicitId);
                if(target.isPresent()) {
                    var old = blocksWithVariants.put(target.get(), mod);
                    if(old != null){
                        Polytone.LOGGER.info("Found 2 Variant Textures jsons with same target ({}). Overriding", explicitId);
                    }
                }
            }
        }
        //no explicit targets. use its own ID instead
        else {
            if(pathTarget.isPresent()) {
                var old = blocksWithVariants.put(pathTarget.get(), mod);
                if(old != null){
                    Polytone.LOGGER.info("Found 2 Variant Textures jsons with same target ({}). Overriding", pathTarget);
                }
            }
        }
    }

    @Override
    protected void reset() {
        blocksWithVariants.clear();
        variantQuadsCache.clear(); //we might need a lock here
        specialOFTintHack.clear();
    }

    public BakedQuad maybeModify(BakedQuad quad, BlockAndTintGetter level, BlockState state, BlockPos pos) {
        if (quad.tintIndex == -1 && !specialOFTintHack.isEmpty()) {
            if (specialOFTintHack.contains(state.getBlock())) quad.tintIndex = 0;
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
        specialOFTintHack.add(block);
    }
}
