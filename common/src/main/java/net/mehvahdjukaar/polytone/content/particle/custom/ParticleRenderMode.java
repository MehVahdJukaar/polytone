package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum ParticleRenderMode implements StringRepresentable {
    TERRAIN,
    SOLID,
    CUTOUT,
    OPAQUE,
    TRANSLUCENT,
    ADDITIVE_TRANSLUCENT,
    INVISIBLE;

    public static final Codec<ParticleRenderMode> CODEC = StringRepresentable.fromEnum(ParticleRenderMode::values);

    public RenderType getBlock() {
        return switch (this) {
            case TERRAIN, SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case ADDITIVE_TRANSLUCENT -> PolytoneRenderTypes.ADDITIVE_TRANSLUCENT_MOVING_BLOCK_RENDERTYPE;
            case TRANSLUCENT -> RenderTypes.cutoutMovingBlock();
            // default was cutout mipped but it no longer exists
            default -> RenderTypes.cutoutMovingBlock();
        };
    }

    public SingleQuadParticle.Layer getLayer(boolean hasModel) {
        if (hasModel) return CUSTOM_LAYER;
        return switch (this) {
            case TERRAIN, SOLID, CUTOUT -> SingleQuadParticle.Layer.TERRAIN;
            case TRANSLUCENT, INVISIBLE -> SingleQuadParticle.Layer.TRANSLUCENT;
            case ADDITIVE_TRANSLUCENT -> ADDITIVE_TRANSLUCENT_LAYER;
            default -> SingleQuadParticle.Layer.OPAQUE;
        };
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public VertexConsumer modifyParticleConsumer(VertexConsumer original) {
        //   if(true)return original;
        if (this == ADDITIVE_TRANSLUCENT) {
            return PolytoneRenderTypes.DEFERRED_BUFFER_SOURCE.getBuffer(
                    PolytoneRenderTypes.ADDITIVE_TRANSLUCENT_PARTICLE_RENDERTYPE);
        } else return original;
    }

    public VertexConsumer modifyBlockConsumer(VertexConsumer original) {
        if (this != ADDITIVE_TRANSLUCENT) return original;
        return PolytoneRenderTypes.DEFERRED_BUFFER_SOURCE.getBuffer(this.getBlock());
    }

    private static final SingleQuadParticle.Layer CUSTOM_LAYER = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.TRANSLUCENT_TERRAIN);
    private static final SingleQuadParticle.Layer ADDITIVE_TRANSLUCENT_LAYER = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, PolytoneRenderTypes.ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE);

}

