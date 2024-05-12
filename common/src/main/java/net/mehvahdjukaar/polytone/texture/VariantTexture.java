package net.mehvahdjukaar.polytone.texture;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import net.mehvahdjukaar.polytone.colormap.CompoundBlockColors;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.mehvahdjukaar.polytone.utils.TargetsHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

// texture to variant texture map
public record VariantTexture(Map<ResourceLocation, Map<ResourceLocation, ResourceLocation>> textures,
                             Optional<Set<ResourceLocation>> explicitTargets) {

    private static final UnboundedMapCodec<ResourceLocation, Map<ResourceLocation, ResourceLocation>> MAP_CODEC = Codec.unboundedMap(ResourceLocation.CODEC,
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC));

    public static final Decoder<VariantTexture> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MAP_CODEC.fieldOf("textures").forGetter(VariantTexture::textures),
                    TargetsHelper.CODEC.optionalFieldOf("targets").forGetter(VariantTexture::explicitTargets)
            ).apply(instance, VariantTexture::new));


    @Nullable
    public Map<ResourceLocation, ResourceLocation> getBiomeMap(TextureAtlasSprite sprite) {
        return textures.get(sprite.contents().name());
    }
}
