package net.mehvahdjukaar.polytone.utils.fabric;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.mehvahdjukaar.polytone.utils.BakedQuadBuilder;
import net.mehvahdjukaar.polytone.utils.BakedQuadsTransformer;
import net.mehvahdjukaar.polytone.utils.fabric.BakedQuadBuilderImpl;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;


public class BakedQuadsTransformerImpl implements BakedQuadsTransformer {

    private Consumer<BakedQuad> inner = i -> {
    };

    private Boolean shade = null;
    private Integer emissivity = null;
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
        inner = inner.andThen(applyingLightmapInplace(packedLight));
        return this;
    }

    @Override
    public BakedQuadsTransformer applyingTransform(Matrix4f transform) {
        inner = inner.andThen(applyingTransformInplace(transform));
        directionRemap = d -> Direction.rotate(new Matrix4f(new Matrix3f(transform)), d);
        return this;
    }

    @Override
    public BakedQuadsTransformer applyingAmbientOcclusion(boolean ambientOcclusion) {
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
        this.emissivity = emissivity;
        return this;
    }

    @Override
    public BakedQuadsTransformer applyingSprite(TextureAtlasSprite sprite) {
        inner = inner.andThen(applyingSpriteInplace(sprite));
        this.sprite = sprite;
        return this;
    }

    private TextureAtlasSprite lastSpriteHack = null;

    @Override
    public BakedQuad transform(BakedQuad quad) {
        int[] v = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);

        int tint = this.tintIndex == null ? quad.getTintIndex() : this.tintIndex;
        boolean shade = this.shade == null ? quad.isShade() : this.shade;
        TextureAtlasSprite sprite = this.sprite == null ? quad.getSprite() : this.sprite;
        lastSpriteHack = quad.getSprite();
        BakedQuad newQuad = new BakedQuad(v, tint, directionRemap.apply(quad.getDirection()), sprite, shade);
        inner.accept(newQuad);
        lastSpriteHack = null;
        if (emissivity != null) {
            BakedQuadBuilder builder = BakedQuadBuilderImpl.create(sprite, null);
            builder.fromVanilla(newQuad);
            builder.lightEmission(emissivity);
            newQuad = builder.build();
        }
        return newQuad;
    }


    private Consumer<BakedQuad> applyingSpriteInplace(TextureAtlasSprite sprite) {
        return q -> {
            TextureAtlasSprite oldSprite = lastSpriteHack;
            int stride = getStride();
            int[] v = q.getVertices();
            float segmentWScale = sprite.contents().width() / (float) oldSprite.contents().width();
            float segmentHScale = sprite.contents().height() / (float) oldSprite.contents().height();

            for (int i = 0; i < 4; i++) {
                int offset = i * stride + UV0;
                float originalU = Float.intBitsToFloat(v[offset]);
                float originalV = Float.intBitsToFloat(v[offset + 1]);

                float u1 = (originalU - oldSprite.getU0()) * segmentWScale;
                v[offset] = Float.floatToRawIntBits(u1 + sprite.getU0());

                float v1 = (originalV - oldSprite.getV0()) * segmentHScale;
                v[offset + 1] = Float.floatToRawIntBits(v1 + sprite.getV0());
            }
        };
    }

    private static Consumer<BakedQuad> applyingColorInplace(IntUnaryOperator indexToABGR) {
        return quad -> {
            int[] v = quad.getVertices();
            int stride = getStride();
            for (int i = 0; i < 4; i++) {
                int i1 = indexToABGR.applyAsInt(i);
                v[i * stride + COLOR] = i1;
            }
        };
    }

    private static Consumer<BakedQuad> applyingLightmapInplace(int packedLight) {
        return quad -> {
            var vertices = quad.getVertices();
            for (int i = 0; i < 4; i++)
                vertices[i * getStride() + UV2] = packedLight;
        };
    }

    private static Consumer<BakedQuad> applyingTransformInplace(Matrix4f transform) {
        return quad -> {
            var v = quad.getVertices();
            int stride = getStride();
            for (int i = 0; i < 4; i++) {
                int offset = i * stride + POSITION;
                float originalX = Float.intBitsToFloat(v[offset]) - 0.5f;
                float originalY = Float.intBitsToFloat(v[offset + 1]) - 0.5f;
                float originalZ = Float.intBitsToFloat(v[offset + 2]) - 0.5f;

                Vector4f vec = new Vector4f(originalX, originalY, originalZ, 1);
                vec.mul(transform);
                // Divide by homogeneous coordinate to obtain transformed 3D point
                vec.div(vec.w);

                v[offset] = Float.floatToRawIntBits(vec.x() + 0.5f);
                v[offset + 1] = Float.floatToRawIntBits(vec.y() + 0.5f);
                v[offset + 2] = Float.floatToRawIntBits(vec.z() + 0.5f);
            }
            var normalTransform = new Matrix3f(transform).invert().transpose();

            for (int i = 0; i < 4; i++) {
                int offset = i * stride + NORMAL;
                int normalIn = v[offset];
                if ((normalIn & 0x00FFFFFF) != 0) {
                    float normalX = ((byte) (normalIn & 0xFF)) / 127.0f;
                    float normalY = ((byte) ((normalIn >> 8) & 0xFF)) / 127.0f;
                    float normalZ = ((byte) ((normalIn >> 16) & 0xFF)) / 127.0f;

                    Vector3f vec = new Vector3f(normalX, normalY, normalZ);
                    vec.mul(normalTransform);
                    vec.normalize();
                    v[offset] = (((byte) (vec.x() * 127.0f)) & 0xFF) |
                            ((((byte) (vec.y() * 127.0f)) & 0xFF) << 8) |
                            ((((byte) (vec.z() * 127.0f)) & 0xFF) << 16) |
                            (normalIn & 0xFF000000);
                }
            }
        };
    }

    private static int getStride() {
        return DefaultVertexFormat.BLOCK.getIntegerSize();
    }

    private static final int POSITION = 0;
    private static final int COLOR = 3;
    private static final int UV0 = 4;
    private static final int UV2 = 6;
    private static final int NORMAL = 7;

}
