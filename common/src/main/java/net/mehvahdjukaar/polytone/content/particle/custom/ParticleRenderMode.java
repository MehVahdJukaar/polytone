package net.mehvahdjukaar.polytone.content.particle.custom;

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
            case ADDITIVE_TRANSLUCENT, TRANSLUCENT -> RenderTypes.translucentMovingBlock();
            // default was cutout mipped but it no longer exists
            default -> RenderTypes.cutoutMovingBlock();
        };
    }

    public SingleQuadParticle.Layer getLayer(boolean hasModel) {
        if (hasModel) return CUSTOM_LAYER;

        // A quad particle's sprite is always baked into the particle atlas (see SpritePicker), so every
        // mode here must bind LOCATION_PARTICLES. The block-atlas variants (terrain/solid/cutout) only
        // apply to the model path via getBlock(); routing a quad through Layer.TERRAIN would sample the
        // whole block atlas with particle-atlas UVs (the "particle shows the block atlas" bug).
        return switch (this) {
            case TERRAIN -> SingleQuadParticle.Layer.OPAQUE_TERRAIN;
            case TRANSLUCENT, INVISIBLE -> SingleQuadParticle.Layer.TRANSLUCENT;
            case ADDITIVE_TRANSLUCENT -> ADDITIVE_TRANSLUCENT_LAYER;
            default -> SingleQuadParticle.Layer.OPAQUE;
        };
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static final SingleQuadParticle.Layer CUSTOM_LAYER = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.TRANSLUCENT_TERRAIN);
    public static final SingleQuadParticle.Layer ADDITIVE_TRANSLUCENT_LAYER = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, PolytoneRenderTypes.ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE);

}

