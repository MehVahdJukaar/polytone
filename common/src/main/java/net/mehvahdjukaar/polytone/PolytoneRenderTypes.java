package net.mehvahdjukaar.polytone;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.polytone.content.particle.custom.render.ModelParticleRenderGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MaterialMapper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.function.Supplier;


public class PolytoneRenderTypes {

    public static void init() {
        PlatStuff.registerParticleGroup(PARTICLE_MODEL_GROUP, new ModelParticleRenderGroup());
    }

    public static final ParticleRenderType PARTICLE_MODEL_GROUP = new ParticleRenderType(Polytone.res("particle_model").toString());

    public static final MaterialMapper PARTICLES_MAPPER = new MaterialMapper(TextureAtlas.LOCATION_PARTICLES, "particle");

    private static final BlendFunction ADDITIVE_TRANSLUCENT_BLEND =
            new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE);


    public static final RenderPipeline ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Polytone.res("pipeline/additive_particle"))
                    .withSampler("Sampler0")
                    .withSampler("Sampler2")
                    .withVertexShader(Identifier.withDefaultNamespace("core/particle"))
                    .withFragmentShader(Identifier.withDefaultNamespace("core/particle")) //so we can use shader define. these shaders are identical
                    .withVertexFormat(DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS)
                    .withShaderDefine("ALPHA_CUTOUT", 0.001F)
                    /* Blending Functions */
                    .withDepthWrite(false) //affects depth buffer
                    .withCull(true) //??
                    .withBlend(ADDITIVE_TRANSLUCENT_BLEND)
                    .build()
    );

    public static final RenderType ADDITIVE_TRANSLUCENT_PARTICLE_RENDERTYPE = RenderType.create(
            Polytone.MOD_ID + ":additive_particle",
            RenderSetup.builder(ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE)
                    .bufferSize(64 * 1024)
                    .useLightmap()
                    .withTexture("Sampler0", PARTICLES_MAPPER.sheet())
                    .setOutline(RenderSetup.OutlineProperty.NONE)
                    .sortOnUpload()
                    .createRenderSetup());


    public static final Supplier<ParticleRenderType> PARTICLE_ADDITIVE_TRANSLUCENCY_RENDER_TYPE = Suppliers.memoize(() ->
            new ParticleRenderType("particle_additive_translucent"));


    //block. Used to render 3d model for particles
    public static final RenderPipeline ADDITIVE_TRANSLUCENT_BLOCK_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
                    .withShaderDefine("ALPHA_CUTOUT", 0.001F)
                    .withLocation(Polytone.res("pipeline/additive_block"))
                    .withBlend(ADDITIVE_TRANSLUCENT_BLEND)
                    .build()
    );

    //in theory same as with particle just different shader?? and texture state
    // Mip map strategy is now part of texture creation.
    public static final RenderType ADDITIVE_TRANSLUCENT_BLOCK_RENDERTYPE = RenderType.create(
            Polytone.MOD_ID + ":additive_block",
            RenderSetup.builder(ADDITIVE_TRANSLUCENT_BLOCK_PIPELINE)
                    .bufferSize(64 * 1024)
                    .affectsCrumbling()
                    .sortOnUpload()
                    .useLightmap()
                    .setOutline(RenderSetup.OutlineProperty.NONE)
                    .createRenderSetup());

    public static final RenderPipeline LEASH_PIPELINE = RenderPipelines.register(RenderPipeline.builder(
                    RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation("polytone/pipeline/leash")
            .withVertexShader("core/rendertype_text")
            .withFragmentShader("core/rendertype_text")
            .withSampler("Sampler2")
            .withSampler("Sampler0")
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.TRIANGLE_STRIP)
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

    public static VertexConsumer getLeashVertexConsumer(MultiBufferSource multiBufferSource) {
        if (!isLeashRenderOn()) return null;
        return multiBufferSource.getBuffer(LEASH_RENDER_TYPE);
    }

    public static boolean addLeashVertexPair(VertexConsumer vertexConsumer, Matrix4f matrix4f,
                                             float startX, float startY, float startZ,

                                             float yOffset,
                                             float dx, float dz,
                                             int index, boolean bl, EntityRenderState.LeashState leashState) {
        if (!isLeashRenderOn()) return false;
        // Calculate segment and interpolate lighting
        float segment = (float) index / 24.0F;
        int blockLight = (int) Mth.lerp(segment, (float) leashState.startBlockLight, (float) leashState.endBlockLight);
        int skyLight = (int) Mth.lerp(segment, (float) leashState.startSkyLight, (float) leashState.endSkyLight);
        int light = LightTexture.pack(blockLight, skyLight);

        // Calculate vertex positions
        float z = startX * segment;
        float aa = startY > 0.0F ? startY * segment * segment : startY - startY * (1.0F - segment) * (1.0F - segment);
        float ab = startZ * segment;

        // Adjust UV coordinates to map correctly across segments
        // U-coordinate should advance with the segment index
        float u1 = 0.0f;     // V-coordinate for the first vertex
        float u2 = 1.0f;     // V-coordinate for the second vertex

        // Apply vertex attributes
        vertexConsumer.addVertex(matrix4f, z - dx, aa, ab + dz)
                .setColor(1, 1, 1, 1f)
                .setLight(light)
                .setUv(u1, segment);

        vertexConsumer.addVertex(matrix4f, z + dx, aa + yOffset, ab - dz)
                .setColor(1, 1, 1, 1f)
                .setLight(light)
                .setUv(u2, segment);


        return true;

    }

    public static void onRenderLast() {
        if (lastModelViewMatrix != null) {
            Matrix4f last = new Matrix4f(RenderSystem.getModelViewMatrix());
            RenderSystem.getModelViewMatrix().set(lastModelViewMatrix);
            DEFERRED_BUFFER_SOURCE.endBatches();
            RenderSystem.getModelViewMatrix().set(last);
        } else {
            DEFERRED_BUFFER_SOURCE.endBatches();
        }
    }

    public static final DeferredBufferSource DEFERRED_BUFFER_SOURCE = new DeferredBufferSource();

    private static Matrix4f lastModelViewMatrix;

    public static void cacheMatrices() {
        lastModelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());
    }

    public static class DeferredBufferSource extends MultiBufferSource.BufferSource {
        protected final Supplier<ByteBufferBuilder> bufferSupplier;

        private final Collection<RenderType> delayed = new HashSet<>();

        protected DeferredBufferSource() {
            this(() -> new ByteBufferBuilder(786432), new LinkedHashMap<>());
        }

        protected DeferredBufferSource(Supplier<ByteBufferBuilder> bufferSupplier, SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers) {
            super(bufferSupplier.get(), fixedBuffers);
            this.bufferSupplier = bufferSupplier;
        }

        public void endBatches() {
            endBatch(ADDITIVE_TRANSLUCENT_BLOCK_RENDERTYPE);
            endBatch(ADDITIVE_TRANSLUCENT_PARTICLE_RENDERTYPE);
            for (RenderType type : delayed) {
                endBatch(type);
            }
        }

        @Override
        public @NotNull VertexConsumer getBuffer(@NotNull RenderType renderType) {
            if (!fixedBuffers.containsKey(renderType)) {
                fixedBuffers.put(renderType, bufferSupplier.get());
                if (renderType != ADDITIVE_TRANSLUCENT_BLOCK_RENDERTYPE && renderType != ADDITIVE_TRANSLUCENT_PARTICLE_RENDERTYPE) {
                    delayed.add(renderType);
                }
            }
            return super.getBuffer(renderType);
        }

        @Override
        public void endBatch(@NotNull RenderType renderType) {
            super.endBatch(renderType);
        }
    }
};

