package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * {@code PostChainMixin} reads this and, when true, rewrites the parsed JSON from the new
     * (1.21.2+) post-effect schema into the old (1.21.1) one so pack-authored shaders written for
     * recent MC versions still load. Anything else creating a PostChain (vanilla, other mods) sees
     * the flag unset and is untouched.
     */
    public static final ThreadLocal<Boolean> POLYTONE_LOADING = ThreadLocal.withInitial(() -> false);

    private final List<PostChainEffect> effects = new ArrayList<>();
    /** Currently loaded chains, in render order. Keyed by effect instance. */
    private final LinkedHashMap<PostChainEffect, PostChain> activeChains = new LinkedHashMap<>();
    /** Post chain IDs we already failed to load; skipped (silently) on subsequent ticks. */
    private final Set<ResourceLocation> failedChains = new HashSet<>();

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
                var result = PostChainEffect.CODEC.parse(JsonOps.INSTANCE, json);
                if (result.isError()) {
                    Polytone.LOGGER.warn("Failed to parse post shader '{}': {}", id, result.error().get().message());
                    continue;
                }
                effects.add(result.getOrThrow());
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
        POLYTONE_LOADING.set(true);
        try {
            Minecraft mc = Minecraft.getInstance();
            PostChain chain = new PostChain(
                    mc.getTextureManager(),
                    mc.getResourceManager(),
                    mc.getMainRenderTarget(),
                    effect.chainResource()
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

    /** Resize all active chains to the new framebuffer dimensions. */
    public void resize(int width, int height) {
        synchronized (effects) {
            for (PostChain c : activeChains.values()) {
                c.resize(width, height);
            }
        }
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
            for (var entry : activeChains.entrySet()) {
                PostChainEffect effect = entry.getKey();
                PostChain chain = entry.getValue();
                try {
                    effect.applyExpressionUniforms(chain);
                    chain.process(partialTicks);
                } catch (Exception e) {
                    Polytone.LOGGER.error("Error processing polytone post chain '{}'", chain.getName(), e);
                }
            }
        }
    }

    private void closeAllChains() {
        for (PostChain c : activeChains.values()) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
        activeChains.clear();
    }
}
