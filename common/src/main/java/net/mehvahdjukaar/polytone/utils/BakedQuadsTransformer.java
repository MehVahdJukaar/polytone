package net.mehvahdjukaar.polytone.utils;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Utility object to transform existing bake quads
 * on Forge just wraps its own QuadTransformers class
 */
public interface BakedQuadsTransformer {

    @ExpectPlatform
    static BakedQuadsTransformer create() {
        throw new AssertionError();
    }

    default List<BakedQuad> transformAll(List<BakedQuad> quads) {
        List<BakedQuad> list = new ArrayList<>();
        quads.forEach(q -> list.add(transform(q)));
        return list;
    }

    BakedQuad transform(BakedQuad quad);

    BakedQuadsTransformer applyingAmbientOcclusion(boolean ambientOcclusion);

    BakedQuadsTransformer applyingEmissivity(int emissivity);

    BakedQuadsTransformer applyingLightMap(int packedLight);

    BakedQuadsTransformer applyingShade(boolean shade);

    BakedQuadsTransformer applyingTintIndex(int tintIndex);

    BakedQuadsTransformer applyingTransform(Matrix4f transform);

    default BakedQuadsTransformer applyingColor(int ABGRcolor) {
        applyingColor(i -> ABGRcolor);
        return this;
    }

    BakedQuadsTransformer applyingColor(IntUnaryOperator indexToABGR);

    BakedQuadsTransformer applyingSprite(TextureAtlasSprite sprite);


}
