package net.mehvahdjukaar.polytone.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Transformation;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.function.Consumer;

/**
 * Cross-loader utility to create baked quad
 * On forge just wraps its own baked quad builder. Can also be fed to render calls as it implements vertex consumer
 */
public interface BakedQuadBuilder extends VertexConsumer {

    static BakedQuadBuilder create(TextureAtlasSprite sprite) {
        return create(sprite, (Matrix4f) null);
    }

    static BakedQuadBuilder create(TextureAtlasSprite sprite, @Nullable Transformation transformation) {
        return create(sprite, transformation == null ? null : new Matrix4f().translate(0.5f, 0.5f, 0.5f)
                .mul(transformation.getMatrix())
                .translate(-0.5f, -0.5f, -0.5f));
    }

    //
    @ExpectPlatform
    static BakedQuadBuilder create(TextureAtlasSprite sprite, @Nullable Matrix4f transformation) {
        throw new AssertionError();
    }


    BakedQuadBuilder setAutoDirection();

    BakedQuadBuilder setDirection(Direction direction);

    BakedQuadBuilder setAmbientOcclusion(boolean ambientOcclusion);

    BakedQuadBuilder setShade(boolean shade);

    BakedQuadBuilder lightEmission(int light);

    @Deprecated(forRemoval = true)
    BakedQuadBuilder fromVanilla(BakedQuad quad);

    BakedQuadBuilder setTint(int tintIndex);

    BakedQuad build();

    BakedQuadBuilder setAutoBuild(Consumer<BakedQuad> quadConsumer);


    @Override
    default BakedQuadBuilder vertex(Matrix4f matrix, float x, float y, float z) {
        VertexConsumer.super.vertex(matrix, x, y, z);
        return this;
    }

}
