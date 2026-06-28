package net.mehvahdjukaar.polytone;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.content.particle.custom.render.ModelParticleRenderGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Matrix4fc;

import static net.minecraft.client.renderer.RenderPipelines.register;


public class PolytoneRenderTypes {

    public static void init() {
        PlatStuff.registerParticleGroup(e -> e.register(PARTICLE_MODEL_GROUP, ModelParticleRenderGroup::new));
    }

    public static final ParticleRenderType PARTICLE_MODEL_GROUP =
            new ParticleRenderType(Polytone.res("particle_model").toString(), "PM");

    // Additive-translucent particle pipeline. Mirrors vanilla TRANSLUCENT_PARTICLE (PARTICLE_SNIPPET bind groups
    // + core/particle vertex shader) but uses Polytone's no-cutoff fragment shader and an additive blend
    // (SRC_ALPHA, ONE). Consumed directly by a SingleQuadParticle.Layer in ParticleRenderMode.
    public static final RenderPipeline ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE = register(
            RenderPipeline.builder()
                    .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .withBindGroupLayout(BindGroupLayouts.FOG)
                    .withVertexShader("core/particle")
                    .withFragmentShader(Polytone.res("core/particle_no_cutoff")) //identical to vanilla core/particle minus the alpha discard
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                    .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .withLocation(Polytone.res("pipeline/additive_particle"))
                    .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE)))
                    .build());


    // Textured leash. Vanilla leashes are untextured colored quads (RenderTypes.leash()); this gives resource packs
    // a textured leash by reusing the in-world text pipeline (core/text, samples Sampler0) as a TRIANGLE_STRIP.
    public static final RenderPipeline LEASH_PIPELINE = register(
            RenderPipeline.builder()
                    .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withBindGroupLayout(BindGroupLayouts.FOG)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
                    .withVertexShader("core/text")
                    .withFragmentShader("core/text")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
                    .withCull(false)
                    .withLocation("polytone/pipeline/leash")
                    .build());

    private static final Identifier LEASH_TEXTURE = Identifier.withDefaultNamespace("textures/entity/lead.png");

    private static final RenderType LEASH_RENDER_TYPE = RenderType.create(
            "polytone_leash",
            RenderSetup.builder(LEASH_PIPELINE)
                    .withTexture("Sampler0", LEASH_TEXTURE)
                    .useLightmap()
                    .createRenderSetup());


    private static boolean isLeashRenderOn() {
        return true;
    }

    public static RenderType getLeashRenderType() {
        if (!isLeashRenderOn()) return null;
        return LEASH_RENDER_TYPE;
    }

    // Mirrors LeashFeatureRenderer.addVertexPair geometry, but writes white-tinted UVs so the leash texture shows.
    public static boolean addLeashVertexPair(VertexConsumer builder, Matrix4fc pose,
                                             float dx, float dy, float dz,
                                             float fudge, float dxOff, float dzOff,
                                             int k, boolean backwards, EntityRenderState.LeashState state) {
        if (!isLeashRenderOn()) return false;

        float progress = k / 24.0F;
        int block = (int) Mth.lerp(progress, state.startBlockLight, state.endBlockLight);
        int sky = (int) Mth.lerp(progress, state.startSkyLight, state.endSkyLight);
        int lightCoords = LightCoordsUtil.pack(block, sky);

        float x = dx * progress;
        float y;
        if (state.slack) {
            y = dy > 0.0F ? dy * progress * progress : dy - dy * (1.0F - progress) * (1.0F - progress);
        } else {
            y = dy * progress;
        }
        float z = dz * progress;

        builder.addVertex(pose, x - dxOff, y + fudge, z + dzOff)
                .setColor(1f, 1f, 1f, 1f)
                .setLight(lightCoords)
                .setUv(0.0f, progress);

        builder.addVertex(pose, x + dxOff, y + 0.05F - fudge, z - dzOff)
                .setColor(1f, 1f, 1f, 1f)
                .setLight(lightCoords)
                .setUv(1.0f, progress);

        return true;
    }
}
