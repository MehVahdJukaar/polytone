package net.mehvahdjukaar.polytone.utils.forge;

import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import net.mehvahdjukaar.polytone.utils.BakedQuadsTransformer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;

public class BakedQuadsTransformerImpl implements BakedQuadsTransformer {

    private IQuadTransformer inner = QuadTransformers.empty();

    private Boolean ambientOcclusion = null;
    private Boolean shade = null;
    private Integer tintIndex = null;
    private UnaryOperator<Direction> directionRemap = UnaryOperator.identity();
    private TextureAtlasSprite sprite = null;

    public static BakedQuadsTransformer create() {
        return new BakedQuadsTransformerImpl();
    }

    @Override
    public BakedQuadsTransformer applyingColor(IntUnaryOperator indexToABGR) {
        inner = inner.andThen(applyingColorInplace(indexToABGR));
        return this;
    }

    @Override
    public BakedQuadsTransformer applyingLightMap(int packedLight) {
        inner = inner.andThen(QuadTransformers.applyingLightmap(packedLight));
        return this;
    }

    @Override
    public BakedQuadsTransformer applyingTransform(Matrix4f transform) {
        //transform to block center
        var m = new Matrix4f();
        m.translate(new Vector3f( -0.5f, -0.5f, -0.5f));
        m.multiply(transform);
        m.translate((new Vector3f(0.5f, 0.5f, 0.5f)));
        inner = inner.andThen(QuadTransformers.applying(new Transformation(m)));
        //directionRemap = d -> Direction.rotate(new Matrix4f(new Matrix3f(transform)), d);
        return this;
    }

    @Override
    public BakedQuadsTransformer applyingAmbientOcclusion(boolean ambientOcclusion) {
        this.ambientOcclusion = ambientOcclusion;
        return this;
    }

    @Override
    public BakedQuadsTransformer applyingShade(boolean shade) {
        this.shade = shade;
        return this;
    }

    @Override
    public BakedQuadsTransformer applyingTintIndex(int tintIndex) {
        this.tintIndex = tintIndex;
        return this;
    }

    @Override
    public BakedQuadsTransformer applyingEmissivity(int emissivity) {
        this.inner = this.inner.andThen(QuadTransformers.settingEmissivity(emissivity));
        return this;
    }

    @Override
    public BakedQuadsTransformer applyingSprite(TextureAtlasSprite sprite) {
        this.inner = this.inner.andThen(applyingSpriteInplace(sprite));
        this.sprite = sprite;
        return this;
    }

    private TextureAtlasSprite lastSpriteHack = null;

    @Override
    public BakedQuad transform(BakedQuad quad) {
        int[] v = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);

        int tint = this.tintIndex == null ? quad.getTintIndex() : this.tintIndex;
        boolean shade = this.shade == null ? quad.isShade() : this.shade;
        boolean ambientOcclusion = this.ambientOcclusion == null ? quad.isShade() : this.shade;
        lastSpriteHack = quad.getSprite();
        TextureAtlasSprite sprite = this.sprite == null ? quad.getSprite() : this.sprite;
        BakedQuad newQuad = new BakedQuad(v, tint, directionRemap.apply(quad.getDirection()), sprite, shade, ambientOcclusion);
        inner.processInPlace(newQuad);
        lastSpriteHack = null;
        return newQuad;
    }


    private IQuadTransformer applyingSpriteInplace(TextureAtlasSprite sprite) {
        return q -> {
            TextureAtlasSprite oldSprite = lastSpriteHack;
            int stride = IQuadTransformer.STRIDE;
            int[] v = q.getVertices();
            float segmentWScale = sprite.getWidth() / (float) oldSprite.getWidth();
            float segmentHScale = sprite.getHeight() / (float) oldSprite.getHeight();

            for (int i = 0; i < 4; i++) {
                int offset = i * stride + IQuadTransformer.UV0;
                float originalU = Float.intBitsToFloat(v[offset]);
                float originalV = Float.intBitsToFloat(v[offset + 1]);

                float u1 = (originalU - oldSprite.getU0()) * segmentWScale;
                v[offset] = Float.floatToRawIntBits(u1 + sprite.getU0());

                float v1 = (originalV - oldSprite.getV0()) * segmentHScale;
                v[offset + 1] = Float.floatToRawIntBits(v1 + sprite.getV0());
            }
        };
    }

    private static IQuadTransformer applyingColorInplace(IntUnaryOperator indexToABGR) {
        return quad -> {
            int[] v = quad.getVertices();
            int stride = IQuadTransformer.STRIDE;
            for (int i = 0; i < 4; i++) {
                int i1 = indexToABGR.applyAsInt(i);
                v[i * stride + IQuadTransformer.COLOR] = i1;
            }
        };
    }

}
