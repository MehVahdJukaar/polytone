package net.mehvahdjukaar.polytone.texture;

import com.mojang.serialization.Codec;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
// texture to variant texture map
public record VariantTexture(Map<ResourceLocation, Map<ResourceLocation, ResourceLocation>> textures) {
    public static final Codec<VariantTexture> CODEC = Codec.unboundedMap(ResourceLocation.CODEC,
                    Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC))
            .xmap(VariantTexture::new, VariantTexture::textures);


    @Nullable
    public Map<ResourceLocation, ResourceLocation> getBiomeMap(TextureAtlasSprite sprite) {
        return textures.get(sprite.contents().name());
    }
}
