package net.mehvahdjukaar.polytone.utils.forge;

import com.google.common.base.Preconditions;
import net.mehvahdjukaar.polytone.utils.BakedQuadBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.function.Consumer;

public class BakedQuadBuilderImpl implements BakedQuadBuilder {

    public static BakedQuadBuilder create(TextureAtlasSprite sprite, @Nullable Matrix4f transformation) {
        return new BakedQuadBuilderImpl(sprite, transformation);
    }

    private final QuadBakingVertexConsumer inner;
    private final TextureAtlasSprite sprite;

    private final Matrix4f globalTransform;
    private final Matrix3f normalTransf;

    private int emissivity = 0;
    private BakedQuad output;
    private boolean autoDirection = false;
    private Consumer<BakedQuad> quadConsumer = s -> output = s;


    private BakedQuadBuilderImpl(TextureAtlasSprite sprite, @Nullable Matrix4f transformation) {
        this.inner = new QuadBakingVertexConsumer(s -> quadConsumer.accept(s));
        this.globalTransform = transformation;// == null ? null : new Matrix4f(new Matrix3f(transformation)); //gets rid of translation
        this.sprite = sprite;
        inner.setShade(true);
        inner.setHasAmbientOcclusion(true);
        inner.setSprite(sprite);
        this.normalTransf = transformation == null ? null :
                new Matrix3f(transformation).invert().transpose();
    }

    @Override
    public BakedQuad build() {
        Preconditions.checkNotNull(output, "vertex data has not been fully filled");
        if (emissivity != 0) {
            QuadTransformers.settingEmissivity(emissivity).processInPlace(output);
        }
        return output;
    }

    @Override
    public BakedQuadBuilderImpl setAutoBuild(Consumer<BakedQuad> quadConsumer) {
        this.quadConsumer = quadConsumer;
        return this;
    }


    @Override
    public BakedQuadBuilderImpl vertex(double x, double y, double z) {
        if (globalTransform != null) {
            inner.vertex(new Matrix4f(globalTransform), (float) x, (float) y, (float) z);
        } else {
            inner.vertex(x, y, z);
        }
        return this;
    }

    @Override
    public BakedQuadBuilderImpl color(int red, int green, int blue, int alpha) {
        inner.color(red, green, blue, alpha);
        return this;
    }

    //given in sprite coords
    @Override
    public BakedQuadBuilderImpl uv(float u, float v) {
        inner.uv(sprite.getU(u * 16), sprite.getV(v * 16));
        return this;
    }

    @Override
    public BakedQuadBuilderImpl overlayCoords(int u, int v) {
        inner.overlayCoords(u, v);
        return this;
    }

    @Override
    public BakedQuadBuilderImpl uv2(int u, int v) {
        inner.uv2(u, v);
        return this;
    }

    @Override
    public BakedQuadBuilderImpl normal(float x, float y, float z) {
        if (globalTransform != null) {
            Vector3f normal = normalTransf.transform(new Vector3f(x, y, z));
            normal.normalize();
            inner.normal(normal.x, normal.y, normal.z);
        } else inner.normal(x, y, z);
        if (autoDirection) {
            this.setDirection(Direction.getNearest(x, y, z));
        }
        return this;
    }

    @Override
    public void endVertex() {
        inner.endVertex();
    }

    @Override
    public void defaultColor(int defaultR, int defaultG, int defaultB, int defaultA) {
        inner.defaultColor(defaultR, defaultG, defaultB, defaultA);
    }

    @Override
    public void unsetDefaultColor() {
        inner.unsetDefaultColor();
    }


    @Override
    public BakedQuadBuilder setDirection(Direction direction) {
        if (globalTransform != null) {
            direction = Direction.rotate(globalTransform, direction);
        }
        this.inner.setDirection(direction);
        return this;
    }

    @Override
    public BakedQuadBuilder setAmbientOcclusion(boolean ambientOcclusion) {
        this.inner.setHasAmbientOcclusion(ambientOcclusion);
        return this;
    }

    @Override
    public BakedQuadBuilder setTint(int tintIndex) {
        inner.setTintIndex(tintIndex);
        return this;
    }

    @Override
    public BakedQuadBuilder setShade(boolean shade) {
        this.inner.setShade(shade);
        return this;
    }

    @Override
    public BakedQuadBuilder lightEmission(int light) {
        this.emissivity = light;
        return this;
    }

    @Override
    public BakedQuadBuilder fromVanilla(BakedQuad q) {
        int[] v = Arrays.copyOf(q.getVertices(), q.getVertices().length);
        output = new BakedQuad(v, q.getTintIndex(), q.getDirection(), q.getSprite(), q.isShade(), q.hasAmbientOcclusion());
        return this;
    }

    @Override
    public BakedQuadBuilder setAutoDirection() {
        this.autoDirection = true;
        return this;
    }
}
