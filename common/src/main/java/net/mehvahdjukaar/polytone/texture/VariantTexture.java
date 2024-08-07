package net.mehvahdjukaar.polytone.texture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import net.mehvahdjukaar.polytone.utils.ITargetProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

// texture to variant texture map
public record VariantTexture(Map<ResourceLocation, Map<ResourceLocation, ResourceLocation>> textures,
                             Set<ResourceLocation> explicitTargets) implements ITargetProvider {

    private static final UnboundedMapCodec<ResourceLocation, Map<ResourceLocation, ResourceLocation>> MAP_CODEC = Codec.unboundedMap(ResourceLocation.CODEC,
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC));

    public static final Decoder<VariantTexture> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MAP_CODEC.fieldOf("textures").forGetter(VariantTexture::textures),
                    TARGET_CODEC.optionalFieldOf("targets", Set.of()).forGetter(VariantTexture::explicitTargets)
            ).apply(instance, VariantTexture::new));


    @Nullable
    public Map<ResourceLocation, ResourceLocation> getBiomeMap(TextureAtlasSprite sprite) {
        return textures.get(sprite.contents().name());
    }
}
