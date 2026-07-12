package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderType;
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

    /** Vanilla block render type used when this particle renders a baked model. */
    public RenderType getBlock() {
        return switch (this) {
            case TERRAIN, SOLID -> RenderType.solid();
            case CUTOUT -> RenderType.cutout();
            case ADDITIVE_TRANSLUCENT -> PolytoneRenderTypes.ADDITIVE_TRANSLUCENT_BLOCK;
            case TRANSLUCENT -> RenderType.translucent();
            case INVISIBLE -> RenderType.cutout();
            // OPAQUE default - cutout mipped, matching the legacy default
            default -> RenderType.cutoutMipped();
        };
    }

    /** Particle sheet used when this particle renders as a plain textured quad. */
    public ParticleRenderType getParticle() {
        return switch (this) {
            case TERRAIN -> ParticleRenderType.TERRAIN_SHEET;
            case TRANSLUCENT -> ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
            case ADDITIVE_TRANSLUCENT -> PolytoneRenderTypes.PARTICLE_ADDITIVE_TRANSLUCENCY_RENDER_TYPE;
            case INVISIBLE -> ParticleRenderType.NO_RENDER;
            // SOLID, CUTOUT, OPAQUE
            default -> ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        };
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public VertexConsumer modifyParticleConsumer(VertexConsumer original) {
        if (this == ADDITIVE_TRANSLUCENT) {
            return PolytoneRenderTypes.DEFERRED_BUFFER_SOURCE.getBuffer(
                    PolytoneRenderTypes.ADDITIVE_TRANSLUCENT_PARTICLE);
        } else return original;
    }

    public VertexConsumer modifyBlockConsumer(VertexConsumer original) {
        return PolytoneRenderTypes.DEFERRED_BUFFER_SOURCE.getBuffer(
                this.getBlock()
        );
    }
}
