package net.mehvahdjukaar.polytone.texture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

// texture to variant texture map
public record VariantTexture(Map<ResourceLocation, Map<ResourceLocation, ResourceLocation>> textures,
                             Targets targets) {

    private static final UnboundedMapCodec<ResourceLocation, Map<ResourceLocation, ResourceLocation>> MAP_CODEC = Codec.unboundedMap(ResourceLocation.CODEC,
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC));

    public static final Decoder<VariantTexture> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MAP_CODEC.fieldOf("textures").forGetter(VariantTexture::textures),
                    Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(VariantTexture::targets)
            ).apply(instance, VariantTexture::new));


    @Nullable
    public Map<ResourceLocation, ResourceLocation> getBiomeMap(TextureAtlasSprite sprite) {
        return textures.get(sprite.contents().name());
    }
}
