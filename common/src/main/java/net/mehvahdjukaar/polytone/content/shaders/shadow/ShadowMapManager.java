package net.mehvahdjukaar.polytone.content.shaders.shadow;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.SingleFileContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.mehvahdjukaar.polytone.content.shaders.IShaderModifier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;

public class ShadowMapManager extends SingleFileContentManager<ShadowMapSettings> implements IShaderModifier {

    public static final String BLOCK_NAME = "PolyShadow";
    public static final String SAMPLER = "InShadow";

    private static final List<String> NAMES = List.of(BLOCK_NAME, SAMPLER);

    private ShadowMapSettings parsedSettings = ShadowMapSettings.DEFAULT;
    private ShadowMapSettings settings = ShadowMapSettings.DEFAULT;
    @Nullable
    private ShadowMap map = null;
    //lazy, the gpu device doesnt exist yet when Polytone statics init
    @Nullable
    private PolyShadowUniforms uniforms = null;
    @Nullable
    private GpuBuffer emptyUniforms = null;
    private final Vector3f cameraFract = new Vector3f();
    private boolean boundThisFrame = false;

    private boolean renderingShadowPass = false;

    public ShadowMapManager() {
        super("Shadow Map", "shadow_map.properties", "shadow_map.json", Polytone.MOD_ID);
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        ShadowMapSettings merged = ShadowMapSettings.DEFAULT;
        for (var entry : resources.jsons().entrySet()) {
            try {
                ShadowMapSettings parsed = ShadowMapSettings.CODEC.parse(ops, entry.getValue()).getOrThrow();
                merged = merged.merge(parsed);
            } catch (Exception e) {
                Polytone.LOGGER.error("Failed to parse shadow_map.json in file {}", entry.getKey(), e);
            }
        }
        this.parsedSettings = merged;
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        settings = parsedSettings;
        closeMap();
        //set by shaders from before the reload, the new ones might not want the map at all
        boundThisFrame = false;
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        parsedSettings = ShadowMapSettings.DEFAULT;
        settings = ShadowMapSettings.DEFAULT;
        closeMap();
        boundThisFrame = false;
    }

    @Override
    public List<String> getEnablingUniforms() {
        return NAMES;
    }

    @Override
    public void bindTo(IShader shader) {
        boundThisFrame = true;
        GpuTextureView shadowTexture = getShadowTexture();
        if (shadowTexture == null) {
            shadowTexture = Minecraft.getInstance().getTextureManager()
                    .getTexture(TextureManager.INTENTIONAL_MISSING_TEXTURE).getTextureView();
        }
        shader.setSampler(SAMPLER, shadowTexture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        GpuBufferSlice slice = getUniformsSlice();
        shader.setUniform(BLOCK_NAME, slice != null ? slice : emptyUniforms());
    }

    //we are inside a render pass here so no writeToBuffer, the zeros go in at creation
    private GpuBufferSlice emptyUniforms() {
        if (emptyUniforms == null) {
            ByteBuffer zeros = MemoryUtil.memCalloc(PolyShadowUniforms.UBO_SIZE);
            emptyUniforms = RenderSystem.getDevice().createBuffer(() -> "Polytone empty shadow UBO",
                    GpuBuffer.USAGE_UNIFORM, zeros);
            MemoryUtil.memFree(zeros);
        }
        return emptyUniforms.slice();
    }

    @Nullable
    public GpuTextureView getShadowTexture() {
        ShadowMap m = map;
        return m == null ? null : m.depthTextureView();
    }

    @Nullable
    public GpuBufferSlice getUniformsSlice() {
        return uniforms == null ? null : uniforms.getSlice();
    }

    //called at renderLevel HEAD, before the post chain frame graph that samples us is built
    public void updateBeforeRenderLevel(GpuBufferSlice shaderFog, Camera camera, Matrix4fc cameraFrustumMatrix, Matrix4f cameraProjectionMatrix) {
        //entity renderers can render whole levels from inside our pass
        if (renderingShadowPass) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (map != null && !map.isFor(level)) closeMap();
        //post chains bind after this runs so they get it a frame late
        boolean wanted = boundThisFrame;
        boundThisFrame = false;
        if (!wanted) return;
        if (level == null || !camera.isInitialized()) return;

        if (map == null) map = new ShadowMap(level, settings.resolution());

        Vec3 camPos = camera.position();
        //every frame, even when the map is reused: the resolve shader's world-grid snap tracks the live camera
        cameraFract.set((float) Mth.frac(camPos.x), (float) Mth.frac(camPos.y), (float) Mth.frac(camPos.z));

        long now = Util.getMillis();
        if (map.canBeReused(camPos, now, settings)) {
            map.realignTo(camPos);
        } else {
            renderingShadowPass = true;
            try {
                float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                Matrix4f cameraViewProjection = cameraProjectionMatrix.mul(cameraFrustumMatrix, new Matrix4f());
                ShadowCasterRenderer.render(mc, level, camera, partialTick, map, settings, cameraViewProjection, shaderFog);
                map.markRendered(camPos, now);
            } catch (Exception e) {
                //the renderer restores its own state in finally blocks, so a failed pass cant leak into renderLevel
                map.markRenderFailed();
                Polytone.LOGGER.error("Polytone shadow map render failed", e);
            } finally {
                renderingShadowPass = false;
            }
        }

        if (uniforms == null) uniforms = new PolyShadowUniforms();
        uniforms.update(map.shadowMatrix(), map.towardLight(), cameraFract);
    }

    private void closeMap() {
        if (map == null) return;
        map.close();
        map = null;
    }

    public void close() {
        closeMap();
        if (uniforms != null) {
            uniforms.close();
            uniforms = null;
        }
        if (emptyUniforms != null) {
            emptyUniforms.close();
            emptyUniforms = null;
        }
    }
}
