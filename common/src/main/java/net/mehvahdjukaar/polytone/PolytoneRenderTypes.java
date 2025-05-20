package net.mehvahdjukaar.polytone;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

import java.util.function.Function;
import java.util.function.Supplier;

public class PolytoneRenderTypes   {

    public static void init() {
    }

    public static final MaterialMapper PARTICLES_MAPPER = new MaterialMapper(TextureAtlas.LOCATION_PARTICLES, "particle");

    public static final RenderPipeline ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder()
                    /* Resource Locations */
                    .withLocation(Polytone.res("pipeline/additive_particle"))
                    .withVertexShader(ResourceLocation.fromNamespaceAndPath("minecraft", "core/particle"))
                    .withFragmentShader(Polytone.res("core/particle_translucent"))
                    .withVertexFormat(DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS)
                    /* Vertex Uniforms */
                    .withUniform("ModelViewMat", UniformType.MATRIX4X4)
                    .withUniform("ProjMat", UniformType.MATRIX4X4)
                    .withUniform("FogShape", UniformType.INT)
                    /* Fragment Uniforms */
                    .withSampler("Sampler0")
                    .withSampler("Sampler2")
                    .withUniform("ColorModulator", UniformType.VEC4)
                    .withUniform("FogStart", UniformType.FLOAT)
                    .withUniform("FogEnd", UniformType.FLOAT)
                    .withUniform("FogColor", UniformType.VEC4)
                    /* Blending Functions */
                    .withDepthWrite(false)
                    .withCull(true)
                    .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE))
                    .build()
    );

    public static final RenderType ADDITIVE_TRANSLUCENT_RENDERTYPE = RenderType.create(
            Polytone.MOD_ID + ":additive_particle",
            4 * 1024 * 1024,
            false,
            true,
            ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE,
            RenderType.CompositeState.builder()
                    .setTextureState(
                            new RenderStateShard.TextureStateShard(
                                    PARTICLES_MAPPER.sheet(),
                                    TriState.FALSE,
                                    false
                            )
                    )
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .createCompositeState(RenderType.OutlineProperty.NONE)
    );

    public static final ParticleRenderType ADDITIVE_TRANSLUCENT_PARTICLE_RENDERTYPE = new ParticleRenderType(
            "ADDITIVE_PARTICLE", ADDITIVE_TRANSLUCENT_RENDERTYPE
    );



    protected static final TransparencyStateShard ADDITIVE_TRANSLUCENT_TRANSPARENCY = new TransparencyStateShard(
            "polytone_additive_transparency",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(
                        GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE
                );
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
    );

    public static final RenderType ADDITIVE_TRANSLUCENT =
                create("polytone_additive_translucent",
                        DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS,
                        786432, true, true,
                        RenderType.CompositeState.builder()
                                .setLightmapState(LIGHTMAP)
                                .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                                .setTextureState(BLOCK_SHEET_MIPPED)
                                .setTransparencyState(ADDITIVE_TRANSLUCENT_TRANSPARENCY)
                                .setOutputState(TRANSLUCENT_TARGET).createCompositeState(true));


    private static final Function<ResourceLocation, RenderType> ADDITIVE_TRANSLUCENT_PARTICLE = Util.memoize((resourceLocation) -> {
        return create("polytone_additive_translucent_particle", DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS,
                1536, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new ShaderStateShard(instance))
                        .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.FALSE, false))
                        .setTransparencyState(ADDITIVE_TRANSLUCENT_TRANSPARENCY)
                        .setOutputState(PARTICLES_TARGET)
                        .setLightmapState(LIGHTMAP)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false));
    });

    public static final Supplier<ParticleRenderType> PARTICLE_ADDITIVE_TRANSLUCENCY_RENDER_TYPE = Suppliers.memoize(() -> {
      return   new ParticleRenderType("PARTICLE_SHEET_ADDITIVE_TRANSLUCENT",
                ADDITIVE_TRANSLUCENT_PARTICLE.apply(TextureAtlas.LOCATION_PARTICLES));
    });

};

