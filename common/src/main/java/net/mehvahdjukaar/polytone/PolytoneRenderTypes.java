package net.mehvahdjukaar.polytone;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL13;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE;
import java.util.function.Supplier;

public class PolytoneRenderTypes extends RenderType {

    static ShaderInstance instance;

    public static void init() {
        PlatStuff.registerShaders(Polytone.res("core/particle_translucent"), DefaultVertexFormat.POSITION_TEX,
                s -> instance = s);
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


    public PolytoneRenderTypes(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }
}