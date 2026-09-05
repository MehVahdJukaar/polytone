package net.mehvahdjukaar.polytone;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.compat.IrisCompat;
import net.mehvahdjukaar.polytone.content.particle.custom.render.ModelParticleRenderGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Matrix4fc;

import java.util.Optional;

import static net.minecraft.client.renderer.RenderPipelines.register;


public class PolytoneRenderTypes {

    public static void init() {
        PlatStuff.registerParticleGroup(e -> e.register(PARTICLE_MODEL_GROUP, ModelParticleRenderGroup::new));
    }

    public static final ParticleRenderType PARTICLE_MODEL_GROUP =
            new ParticleRenderType(Polytone.res("particle_model").toString(), "PM");

    private static final BlendFunction ADDITIVE_TRANSLUCENT_BLEND = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE);

    public static final RenderPipeline ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE = register(
            RenderPipeline.builder()
                    .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .withBindGroupLayout(BindGroupLayouts.FOG)
                    .withVertexShader("core/particle")
                    .withFragmentShader(Polytone.res("core/particle_no_cutoff"))
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                    .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .withLocation(Polytone.res("pipeline/additive_particle"))
                    .withColorTargetState(new ColorTargetState(ADDITIVE_TRANSLUCENT_BLEND))
                    .build());

    public static final RenderPipeline ADDITIVE_TRANSLUCENT_BLOCK_PIPELINE = register(
            RenderPipeline.builder()
                    .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                    .withBindGroupLayout(BindGroupLayouts.FOG)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .withVertexShader("core/block")
                    .withFragmentShader("core/block")
                    .withVertexBinding(0, DefaultVertexFormat.BLOCK)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .withLocation(Polytone.res("pipeline/additive_translucent_moving_block"))
                    .withColorTargetState(new ColorTargetState(ADDITIVE_TRANSLUCENT_BLEND))
                    .build());

    public static final RenderType ADDITIVE_TRANSLUCENT_MOVING_BLOCK_RENDERTYPE = RenderType.create(Polytone.MOD_ID + ":additive_translucent_moving_block",
            RenderSetup.builder(ADDITIVE_TRANSLUCENT_BLOCK_PIPELINE)
                    .useLightmap()
                    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS,
                            () -> RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, true))
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .sortOnUpload()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup());


    public static final RenderPipeline SKY_DEPTH_WRITE_PIPELINE = register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Polytone.res("pipeline/sky_depth"))
                    .withVertexShader("core/sky")
                    .withFragmentShader("core/sky")
                    .withVertexBinding(0, DefaultVertexFormat.POSITION)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
                    .build());

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
                    .withLocation(Polytone.res("pipeline/leash"))
                    .build());

    public static final RenderPipeline DEPTH_COMBINE_PIPELINE = register(
            RenderPipeline.builder()
                    .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                    .withLocation(Polytone.res("pipeline/depth_combine"))
                    .withVertexShader("core/screenquad")
                    .withFragmentShader(Polytone.res("core/depth_combine"))
                    .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
                    .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_NONE))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build());

    private static final Identifier LEASH_TEXTURE = Identifier.withDefaultNamespace("textures/entity/lead.png");

    private static final RenderType LEASH_RENDER_TYPE = RenderType.create(
            "polytone_leash",
            RenderSetup.builder(LEASH_PIPELINE)
                    .withTexture("Sampler0", LEASH_TEXTURE)
                    .useLightmap()
                    .createRenderSetup());


    // TODO: leashes need their own pipeline under Iris. Copying vanilla TEXT gets us a GLYPH program that
    // doesn't shade leashes right, so for now we just fall back to vanilla leashes while shaders are on.
    private static boolean isLeashRenderOn() {
        return !CompatHandler.IRIS || !IrisCompat.isIrisRenderOn();
    }

    public static RenderType getLeashRenderType() {
        if (!isLeashRenderOn()) return null;
        return LEASH_RENDER_TYPE;
    }

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
