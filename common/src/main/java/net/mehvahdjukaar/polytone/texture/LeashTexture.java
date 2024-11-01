package net.mehvahdjukaar.polytone.texture;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.polytone.IrisCompat;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.TriState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class LeashTexture extends RenderType {

    private static final ResourceLocation LEASH_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/lead.png");

    private static final RenderType RENDER_TYPE = RenderType.create("polytone_leash", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.TRIANGLE_STRIP, 1536, RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(LEASH_TEXTURE, TriState.FALSE, false))
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP).createCompositeState(false));

    public LeashTexture(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }


    @Nullable
    public static VertexConsumer getVertexConsumer(MultiBufferSource multiBufferSource) {
        if (Polytone.iris && IrisCompat.isIrisShaderFuckerActive()) return null;
        return multiBufferSource.getBuffer(RENDER_TYPE);
    }

    public static boolean addVertexPair(VertexConsumer vertexConsumer, Matrix4f matrix4f,
                                        float startX, float startY, float startZ,
                                        int blockLight0, int blockLight1, int skyLight0, int skylight1,
                                        float y0, float y1,
                                        float dx, float dz,
                                        int index, boolean flippedColors) {
        if (Polytone.iris && IrisCompat.isIrisShaderFuckerActive()) return false;

        // Calculate segment and interpolate lighting
        float segment = (float) index / 24.0F;
        int blockLight = (int) Mth.lerp(segment, (float) blockLight0, (float) blockLight1);
        int skyLight = (int) Mth.lerp(segment, (float) skyLight0, (float) skylight1);
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
        vertexConsumer.addVertex(matrix4f, z - dx, aa + y1, ab + dz)
                .setColor(1, 1, 1, 1f).setLight(light)
                .setUv(u1, segment);

        vertexConsumer.addVertex(matrix4f, z + dx, aa + y0 - y1, ab - dz)
                .setColor(1, 1, 1, 1f).setLight(light)
                .setUv(u2, segment);


        return true;
    }
}
