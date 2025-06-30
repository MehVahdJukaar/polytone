package net.mehvahdjukaar.polytone;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MaterialMapper;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

import java.util.function.Supplier;

import static net.minecraft.client.renderer.RenderStateShard.LIGHTMAP;

public class PolytoneRenderTypes {

    public static void init() {
    }

    public static final MaterialMapper PARTICLES_MAPPER = new MaterialMapper(TextureAtlas.LOCATION_PARTICLES, "particle");

    private static final BlendFunction ADDITIVE_TRANSLUCENT_BLEND =
            new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE);


    public static final RenderPipeline ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_FOG_SNIPPET)
                    .withLocation(Polytone.res("pipeline/additive_particle"))
                    .withSampler("Sampler0")
                    .withSampler("Sampler2")
                    .withVertexShader(ResourceLocation.withDefaultNamespace("core/particle"))
                    .withFragmentShader(ResourceLocation.withDefaultNamespace("core/terrain")) //so we can use shader define. these shaders are identical
                    .withVertexFormat(DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS)
                    .withShaderDefine("ALPHA_CUTOUT", 0.001F)
                    /* Blending Functions */
                    .withDepthWrite(false) //??
                    .withCull(true) //??
                    .withBlend(ADDITIVE_TRANSLUCENT_BLEND)
                    .build()
    );

    public static final RenderType ADDITIVE_TRANSLUCENT_PARTICLE_RENDERTYPE = RenderType.create(
            Polytone.MOD_ID + ":additive_particle",
            64 * 1024, //?? translucent particles uses 1536
            false, //??
            true, //??
            ADDITIVE_TRANSLUCENT_PARTICLE_PIPELINE,
            RenderType.CompositeState.builder()
                    .setTextureState(
                            new RenderStateShard.TextureStateShard(
                                    PARTICLES_MAPPER.sheet(),
                                    TriState.FALSE,
                                    false
                            )
                    )
                    .setLightmapState(LIGHTMAP)
                   // .setOutputState(RenderStateShard.PARTICLES_TARGET) //??
                    .createCompositeState(RenderType.OutlineProperty.NONE)
    );


    public static final Supplier<ParticleRenderType> PARTICLE_ADDITIVE_TRANSLUCENCY_RENDER_TYPE = Suppliers.memoize(() ->
            new ParticleRenderType("PARTICLE_SHEET_ADDITIVE_TRANSLUCENT",
                    ADDITIVE_TRANSLUCENT_PARTICLE_RENDERTYPE));


    //block. Used to render 3d model for particles
    public static final RenderPipeline ADDITIVE_TRANSLUCENT_BLOCK_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
                    .withShaderDefine("ALPHA_CUTOUT", 0.001F)
                    .withLocation(Polytone.res("pipeline/additive_block"))
                    .withBlend(ADDITIVE_TRANSLUCENT_BLEND)
                    .build()
    );

    //in theory same as with particle just different shader?? and texture state
    public static final RenderType ADDITIVE_TRANSLUCENT_BLOCK_RENDERTYPE = RenderType.create(
            Polytone.MOD_ID + ":additive_block",
            1024*64, //no idea
            true, //crumbling ??
            true, //sorted
            ADDITIVE_TRANSLUCENT_BLOCK_PIPELINE,
            RenderType.CompositeState.builder()
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
                  //  .setOutputState(RenderStateShard.PARTICLES_TARGET)
                    .createCompositeState(RenderType.OutlineProperty.NONE));


};

