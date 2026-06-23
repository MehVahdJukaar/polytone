package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;

/**
 * Manages Polytone-defined post-shader effects.
 *
 * <p>Effects are defined via {@code assets/<ns>/polytone/post_shaders/*.json} and refer to vanilla
 * post-chain JSONs at {@code assets/<ns>/shaders/post/<name>.json}.</p>
 *
 * <p>Polytone post-chains are processed as additional passes after the vanilla
 * {@code gameRenderer.postEffect}, never replacing or modifying it — so other mods
 * that toggle the vanilla post effect (e.g. spectator-mode shaders) keep working.</p>
 */
public class PostShadersManager extends JsonPartialReloader {

    /**
     * Set on the render thread while we're constructing a {@link PostChain} for a polytone effect.
     * Loader-side mixins (e.g. fabric {@code EffectInstanceMixin}) read this and only intervene when
     * it's true, so vanilla and other mods' shader loading is left strictly untouched.
     */
    public static final ThreadLocal<Boolean> POLYTONE_LOADING = ThreadLocal.withInitial(() -> false);

    private final List<PostChainEffect> effects = new ArrayList<>();
    /** Currently loaded chains, in render order. Keyed by effect instance. */
    private final LinkedHashMap<PostChainEffect, PostChain> activeChains = new LinkedHashMap<>();
    /** Post chain IDs we already failed to load; skipped (silently) on subsequent ticks. */
    private final Set<ResourceLocation> failedChains = new HashSet<>();

    /**
     * Level projection / camera matrices captured during {@code GameRenderer.renderLevel}, exposed to
     * pass shaders as the {@code PolyProjMat} / {@code PolyModelViewMat} built-in uniforms.
     */
    private final Matrix4f projMat = new Matrix4f();
    private final Matrix4f modelViewMat = new Matrix4f();

    /**
     * Standalone depth target for effects that declare {@code use_depth_buffer}. We can't sample the
     * main framebuffer's own depth attachment while the post quad writes to it (read/write feedback
     * loop), so we blit the level depth into this snapshot once per frame and sample that instead.
     */
    private TextureTarget depthSnapshot = null;

    public PostShadersManager() {
        super("post_shaders");
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, RegistryAccess access) {
        synchronized (effects) {
            closeAllChains();
            effects.clear();
            failedChains.clear();
            for (var e : jsons.entrySet()) {
                ResourceLocation id = e.getKey();
                JsonElement json = e.getValue();
                try {
                    var result = PostChainEffect.CODEC.parse(JsonOps.INSTANCE, json);
                    if (result.isError()) {
                        Polytone.LOGGER.warn("Failed to parse post shader '{}': {}", id, result.error().get().message());
                        continue;
                    }
                    effects.add(result.getOrThrow());
                } catch (Exception ex) {
                    // Belt-and-suspenders: never let one bad polytone post_shaders/*.json file
                    // abort the reload for the rest of the pack.
                    Polytone.LOGGER.warn("Failed to parse post shader '{}': {}", id, ex.getMessage());
                }
            }
        }
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        synchronized (effects) {
            closeAllChains();
            effects.clear();
            failedChains.clear();
        }
    }

    /** Evaluate conditions and create/close chains accordingly. Call once per client tick. */
    public void tick() {
        synchronized (effects) {
            // Build the desired set (in render order) honouring priority
            List<PostChainEffect> desired = new ArrayList<>();
            for (PostChainEffect e : effects) {
                if (e.shouldBeOn()) desired.add(e);
            }
            desired.sort(Comparator.comparing(PostChainEffect::priority));

            // Close any chains we no longer want
            Map<PostChainEffect, PostChain> keep = new IdentityHashMap<>();
            for (var entry : activeChains.entrySet()) {
                if (desired.contains(entry.getKey())) {
                    keep.put(entry.getKey(), entry.getValue());
                } else {
                    try { entry.getValue().close(); } catch (Exception ignored) {}
                }
            }

            // Open any new chains, preserving desired order
            activeChains.clear();
            for (PostChainEffect e : desired) {
                if (failedChains.contains(e.postChain())) continue; // skip — already logged once
                PostChain chain = keep.get(e);
                if (chain == null) {
                    chain = tryLoadChain(e);
                    if (chain == null) {
                        failedChains.add(e.postChain());
                        continue;
                    }
                }
                activeChains.put(e, chain);
            }
        }
    }

    private PostChain tryLoadChain(PostChainEffect effect) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation chainRl = effect.chainResource();

        // Peek at the chain JSON first so we can reject 1.21.2+ format packs with a clear message
        // instead of letting vanilla blow up with an opaque parse error mid-load.
        Optional<Resource> res = mc.getResourceManager().getResource(chainRl);
        if (res.isEmpty()) {
            Polytone.LOGGER.error("Post shader chain '{}' not found at {}", effect.postChain(), chainRl);
            return null;
        }
        try (Reader reader = res.get().openAsReader()) {
            JsonObject root = GsonHelper.parse(reader);
            if (isNewFormatChain(root)) {
                Polytone.LOGGER.error(
                        "Post shader chain '{}' uses the 1.21.2+ post_effect format (fragment_shader / object-shaped targets / UBO-grouped uniforms). " +
                                "Polytone for 1.21.1 only supports the 1.21.1 post-chain format (passes with 'name', flat 'uniforms' array, 'shaders/program/...json' programs). " +
                                "Either downgrade the pack's shaders to the 1.21.1 format or use a polytone build for the matching MC version. Skipping this chain.",
                        effect.postChain());
                return null;
            }
        } catch (Exception ex) {
            Polytone.LOGGER.error("Failed to read post shader chain '{}': {}", effect.postChain(), ex.getMessage());
            return null;
        }

        POLYTONE_LOADING.set(true);
        try {
            PostChain chain = new PostChain(
                    mc.getTextureManager(),
                    mc.getResourceManager(),
                    mc.getMainRenderTarget(),
                    chainRl
            );
            chain.resize(mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
            return chain;
        } catch (IOException ex) {
            Polytone.LOGGER.error("Failed to load post shader chain '{}': {}", effect.postChain(), ex.getMessage());
            return null;
        } catch (Exception ex) {
            Polytone.LOGGER.error("Failed to parse post shader chain '{}'", effect.postChain(), ex);
            return null;
        } finally {
            POLYTONE_LOADING.set(false);
        }
    }

    /**
     * Detects the 1.21.2+ post-effect schema. Signals: {@code targets} is a JSON object (rather
     * than an array of strings/objects), or any pass declares {@code fragment_shader} (the new
     * format's replacement for the 1.21.1 {@code name} field).
     */
    private static boolean isNewFormatChain(JsonObject root) {
        JsonElement targets = root.get("targets");
        if (targets != null && targets.isJsonObject()) return true;
        JsonElement passes = root.get("passes");
        if (passes != null && passes.isJsonArray()) {
            for (JsonElement p : passes.getAsJsonArray()) {
                if (p.isJsonObject() && p.getAsJsonObject().has("fragment_shader")) return true;
            }
        }
        return false;
    }

    /** Resize all active chains to the new framebuffer dimensions. */
    public void resize(int width, int height) {
        synchronized (effects) {
            for (PostChain c : activeChains.values()) {
                c.resize(width, height);
            }
            if (depthSnapshot != null) {
                depthSnapshot.resize(width, height, Minecraft.ON_OSX);
            }
        }
    }

    /**
     * Capture the level projection and camera (model-view) matrices. Called from the
     * {@code GameRenderer.renderLevel} mixin so the {@code PolyProjMat} / {@code PolyModelViewMat}
     * built-in uniforms reflect the current frame's view.
     */
    public void captureLevelMatrices(Matrix4f projection, Matrix4f modelView) {
        this.projMat.set(projection);
        this.modelViewMat.set(modelView);
    }

    /**
     * Process all active Polytone post-shader chains. Each chain reads from and writes back to the
     * main render target, so subsequent chains see the previous chain's output.
     *
     * <p>Called from the {@code GameRenderer.render} mixin after vanilla's post-effect (if any) finishes.</p>
     */
    public void renderAfterMainPostEffect(float partialTicks) {
        synchronized (effects) {
            if (activeChains.isEmpty()) return;

            Minecraft mc = Minecraft.getInstance();
            float sunAngle = 0f;
            float dayTime = 0f;
            ClientLevel level = mc.level;
            if (level != null) {
                float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
                // match 1.21.11: 0 = noon (sun straight up), measured from the horizon
                sunAngle = level.getSunAngle(partial) - Mth.HALF_PI;
                dayTime = (float) (level.getDayTime() % 24000L);
            }

            IntSupplier depthTexture = prepareDepthSnapshot(mc);

            for (var entry : activeChains.entrySet()) {
                PostChainEffect effect = entry.getKey();
                PostChain chain = entry.getValue();
                try {
                    effect.applyUniforms(chain, projMat, modelViewMat, sunAngle, dayTime, depthTexture);
                    chain.process(partialTicks);
                } catch (Exception e) {
                    Polytone.LOGGER.error("Error processing polytone post chain '{}'", chain.getName(), e);
                }
            }
        }
    }

    /**
     * If any active effect samples the depth buffer, blit the main render target's depth into a private
     * snapshot target and return a supplier of its depth texture id. Returns {@code null} when no active
     * effect needs depth (so the snapshot target is never allocated for packs that don't use it).
     */
    private IntSupplier prepareDepthSnapshot(Minecraft mc) {
        boolean needed = false;
        for (PostChainEffect e : activeChains.keySet()) {
            if (e.useDepthBuffer()) {
                needed = true;
                break;
            }
        }
        if (!needed) return null;

        RenderTarget main = mc.getMainRenderTarget();
        if (depthSnapshot == null) {
            depthSnapshot = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            depthSnapshot.setClearColor(0f, 0f, 0f, 0f);
        } else if (depthSnapshot.width != main.width || depthSnapshot.height != main.height) {
            depthSnapshot.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        depthSnapshot.copyDepthFrom(main);
        return depthSnapshot::getDepthTextureId;
    }

    private void closeAllChains() {
        for (PostChain c : activeChains.values()) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
        activeChains.clear();
        if (depthSnapshot != null) {
            depthSnapshot.destroyBuffers();
            depthSnapshot = null;
        }
    }
}
