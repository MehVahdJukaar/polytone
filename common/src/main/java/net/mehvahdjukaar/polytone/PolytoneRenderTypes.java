package net.mehvahdjukaar.polytone;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

import java.util.function.Function;

public class PolytoneRenderTypes extends RenderType {

    static ShaderInstance instance;

    public PolytoneRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void init() {
        PlatStuff.registerShaders(Polytone.res("particle_translucent"), DefaultVertexFormat.POSITION_TEX,
                ShaderDefines.EMPTY, s -> instance = s);
    }

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

    private static final Function<ResourceLocation, RenderType> ADDITIVE_TRANSLUCENCY = Util.memoize((resourceLocation) -> {
        return create("polytone_additive_translucent_particle", DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS,
                1536, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(PARTICLE_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.FALSE, false))
                        .setTransparencyState(ADDITIVE_TRANSLUCENT_TRANSPARENCY)
                        .setOutputState(PARTICLES_TARGET)
                        .setLightmapState(LIGHTMAP)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false));
    });

    public static final ParticleRenderType PARTICLE_ADDITIVE_TRANSLUCENCY_RENDER_TYPE =
            new ParticleRenderType("PARTICLE_SHEET_ADDITIVE_TRANSLUCENT",
                    ADDITIVE_TRANSLUCENCY.apply(TextureAtlas.LOCATION_PARTICLES));

};

