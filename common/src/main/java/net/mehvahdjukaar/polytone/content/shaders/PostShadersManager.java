package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;
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
 * Manages Polytone-defined post-shader effects: loads them, evaluates their conditions each tick, and
 * opens/closes the underlying {@link PostChain}s. The per-frame GPU work (depth snapshot, matrices,
 * running the chains) lives in {@link PostShaderRenderer}, which this class drives.
 *
 * <p>Effects are defined via {@code assets/<ns>/polytone/post_chains/*.json} (or the legacy
 * {@code post_shaders} folder) and refer to chain JSONs at {@code assets/<ns>/post_effect/<name>.json},
 * the same location 1.21.11 uses, so packs can share files.</p>
 *
 * <p>Polytone post-chains are processed as additional passes after the vanilla
 * {@code gameRenderer.postEffect}, never replacing or modifying it - so other mods
 * that toggle the vanilla post effect (e.g. spectator-mode shaders) keep working.</p>
 */
public class PostShadersManager extends JsonPartialReloader<PostChainEffect> {

    /**
     * Set on the render thread while we're constructing a {@link PostChain} for a polytone effect.
     * Loader-side mixins (e.g. fabric {@code EffectInstanceMixin}) read this and only intervene when
     * it's true, so vanilla and other mods' shader loading is left strictly untouched.
     */
    public static final ThreadLocal<Boolean> POLYTONE_LOADING = ThreadLocal.withInitial(() -> false);

    /**
     * Per-pass frame state for {@link net.mehvahdjukaar.polytone.mixins.PostPassMixin}. Set by
     * {@link PostShaderRenderer} for the duration of each {@code PostChain.process()} call so uniforms
     * are applied immediately before {@code EffectInstance.apply()} on every pass.
     */
    public static final ThreadLocal<ActivePostPassFrame> ACTIVE_POST_PASS = new ThreadLocal<>();

    private final List<PostChainEffect> effects = new ArrayList<>();
    // Currently loaded chains, in render order, keyed by effect instance.
    private final LinkedHashMap<PostChainEffect, PostChain> activeChains = new LinkedHashMap<>();
    // Chain IDs we already failed to load; skipped silently on subsequent ticks.
    private final Set<ResourceLocation> failedChains = new HashSet<>();

    private final PostShaderRenderer renderer = new PostShaderRenderer();

    public PostShadersManager() {
        super(Spec.of("Post shader", () -> PostChainEffect.CODEC)
                .wikiPage("Shaders")
                .folders("post_chains", "post_shaders"));
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
            Polytone.POST_TARGETS.close();
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
                if (failedChains.contains(e.postChain())) continue; // skip - already logged once
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

    // Detects the 1.21.2+ post-effect schema: a `targets` JSON object (not an array), or a pass
    // declaring `fragment_shader` (the new format's replacement for the 1.21.1 `name` field).
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

    /** Resize all active chains, plus the renderer's depth snapshot, to the new framebuffer dimensions. */
    public void resize(int width, int height) {
        synchronized (effects) {
            for (PostChain c : activeChains.values()) {
                c.resize(width, height);
            }
            renderer.resize(width, height);
        }
    }

    /** @see PostShaderRenderer#captureLevelMatrices */
    public void captureLevelMatrices(Matrix4f projection, Matrix4f modelView) {
        renderer.captureLevelMatrices(projection, modelView);
    }

    /** @see PostShaderRenderer#captureLevelDepthSnapshot */
    public void captureLevelDepthSnapshot() {
        synchronized (effects) {
            if (!anyActiveEffectUsesDepth()) return;
            renderer.captureLevelDepthSnapshot();
        }
    }

    /** @see PostShaderRenderer#render */
    public void renderAfterMainPostEffect(float partialTicks) {
        synchronized (effects) {
            if (activeChains.isEmpty()) return;
            renderer.render(activeChains, anyActiveEffectUsesDepth(), partialTicks);
        }
    }

    private boolean anyActiveEffectUsesDepth() {
        for (PostChainEffect e : activeChains.keySet()) {
            if (e.useDepthBuffer()) return true;
        }
        return false;
    }

    /**
     * Whether any active chain declares {@code use_shadow_map}. Read from the render thread
     * ({@code LevelRenderer.renderLevel} TAIL) to decide whether to render the shadow depth map.
     */
    public boolean anyActiveEffectUsesShadowMap() {
        synchronized (effects) {
            for (PostChainEffect e : activeChains.keySet()) {
                if (e.useShadowMap()) return true;
            }
            return false;
        }
    }

    public record ActivePostPassFrame(
            PostChainEffect effect,
            Matrix4f projMat,
            Matrix4f modelViewMat,
            float sunAngle,
            float dayTime,
            float deltaTime,
            BlockPos playerBlockPos,
            Vec3 playerOffset,
            IntSupplier depthTexture
    ) {}

    private void closeAllChains() {
        for (PostChain c : activeChains.values()) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
        activeChains.clear();
        renderer.close();
        Polytone.SHADOWS.renderer().close();
    }
}
