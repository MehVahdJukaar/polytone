package net.mehvahdjukaar.polytone.content.shaders.shadow;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.SingleFileContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.content.shaders.IShaderModifier;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPassTracker;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;

public class ShadowMapManager extends SingleFileContentManager<Void> implements IShaderModifier {

    public static final String SAMPLER = "InShadow";
    public static final String MATRIX = "PolyShadowMat";
    public static final String LIGHT_DIR = "PolyShadowLightDir";
    public static final String CAMERA_FRACT = "PolyShadowCamFract";

    private static final List<String> UNIFORM_NAMES = List.of(MATRIX, LIGHT_DIR, CAMERA_FRACT);
    private static final Matrix4f NO_SHADOW_MATRIX = new Matrix4f();

    private ShadowMapSettings parsedSettings = ShadowMapSettings.DEFAULT;
    private ShadowMapSettings settings = ShadowMapSettings.DEFAULT;
    @Nullable
    private ShadowMap map = null;

    private boolean renderingShadowPass = false;
    private boolean boundThisFrame = false;

    public ShadowMapManager() {
        super("Shadow Map", "shadow_map.properties", "shadow_map.json", Polytone.MOD_ID);
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
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
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        settings = parsedSettings;
        closeMap();
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        parsedSettings = ShadowMapSettings.DEFAULT;
        settings = ShadowMapSettings.DEFAULT;
        closeMap();
    }

    @Override
    public List<String> getEnablingUniforms() {
        return UNIFORM_NAMES;
    }

    @Override
    public void bindTo(IShader inputs) {
        boundThisFrame = true;
        //every frame, even when the map is reused: the resolve shader's world-grid snap tracks the live camera
        Vec3 camPos = LevelRenderPassTracker.currentCamera().getPosition();
        inputs.getUniform(CAMERA_FRACT).set((float) Mth.frac(camPos.x), (float) Mth.frac(camPos.y), (float) Mth.frac(camPos.z));

        ShadowMap m = mapForCurrentPass();
        if (m == null) {
            inputs.setSampler(SAMPLER, 0);
            inputs.getUniform(MATRIX).set(NO_SHADOW_MATRIX);
            inputs.getUniform(LIGHT_DIR).set(0f, 1f, 0f);
        } else {
            inputs.setSampler(SAMPLER, m.depthTextureId());
            inputs.getUniform(MATRIX).set(m.shadowMatrix());
            inputs.getUniform(LIGHT_DIR).set(m.towardLight().x, m.towardLight().y, m.towardLight().z);
        }
    }

    @Nullable
    private ShadowMap mapForCurrentPass() {
        ShadowMap m = map;
        if (m == null || !m.isFor(LevelRenderPassTracker.currentLevel())) return null;
        return m;
    }

    //main pass only, LevelRendererMixin filters the rest
    public void updateAfterRenderLevel(Camera camera, Matrix4f cameraFrustumMatrix, Matrix4f cameraProjectionMatrix) {
        if (renderingShadowPass) return;
        boolean wanted = boundThisFrame;
        boundThisFrame = false;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (map != null && !map.isFor(level)) closeMap();
        if (!wanted || level == null || !camera.isInitialized()) return;

        if (map == null) map = new ShadowMap(level, settings.resolution());

        Vec3 camPos = camera.getPosition();
        long now = Util.getMillis();
        if (map.canBeReused(camPos, now, settings)) {
            map.realignTo(camPos);
            return;
        }

        renderingShadowPass = true;
        try {
            float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
            Matrix4f cameraViewProjection = cameraProjectionMatrix.mul(cameraFrustumMatrix, new Matrix4f());
            ShadowCasterRenderer.render(mc, level, camera, partialTick, map, settings, cameraViewProjection);
            map.markRendered(camPos, now);
        } catch (Exception e) {
            //the renderer restores its own GL state in finally blocks, so a failed pass cant leak into renderLevel
            map.markRenderFailed();
            Polytone.LOGGER.error("Polytone shadow map render failed", e);
        } finally {
            renderingShadowPass = false;
        }
    }

    private void closeMap() {
        if (map == null) return;
        map.close();
        map = null;
    }
}
