package net.mehvahdjukaar.polytone.content.shaders.post;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PostChainsManager extends ContentManager<PostChainEffect> {

    private final List<PostChainEffect> effects = new ArrayList<>();
    private Map<PostChainEffect, PostChain> activeChains = new LinkedHashMap<>();
    private final Set<ResourceLocation> failedChains = new HashSet<>();

    private final LevelDepthSnapshot depthSnapshot = new LevelDepthSnapshot();
    private boolean loadingChain = false;
    @Nullable
    private PostChainEffect processingEffect = null;

    public PostChainsManager() {
        super(Spec.of("Post shader", () -> PostChainEffect.CODEC)
                .wikiPage("Shaders")
                .folders("post_chains", "post_shaders"));
    }

    public boolean isLoadingChain() {
        return loadingChain;
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        closeEverything();
        for (var e : parseEnabledJsons(resources.jsons(), ops)) {
            effects.add(e.getValue());
        }
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        closeEverything();
    }

    public void tick() {
        List<PostChainEffect> wantedEffects = new ArrayList<>();
        for (PostChainEffect e : effects) {
            // skip failed ones, alredy logged once
            if (e.shouldBeOn() && !failedChains.contains(e.postChain())) wantedEffects.add(e);
        }
        wantedEffects.sort(Comparator.comparing(PostChainEffect::priority));

        Map<PostChainEffect, PostChain> nextActiveChains = new LinkedHashMap<>();
        for (PostChainEffect e : wantedEffects) {
            PostChain chain = activeChains.remove(e);
            if (chain == null) chain = tryLoadChain(e);

            if (chain == null) failedChains.add(e.postChain());
            else nextActiveChains.put(e, chain);
        }
        //leftovers are no longer wanted
        closeActiveChains();
        activeChains = nextActiveChains;
    }

    @Nullable
    private PostChain tryLoadChain(PostChainEffect effect) {
        if (!isLoadableLegacyFormatChain(effect)) return null;

        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        //passes look up targets while the chain loads so ours must exist already
        Polytone.POST_TARGETS.ensureAllocated(main.width, main.height);
        loadingChain = true;
        try {
            PostChain chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(),
                    main, effect.chainResource());
            chain.resize(main.width, main.height);
            return chain;
        } catch (Exception ex) {
            Polytone.LOGGER.error("Failed to load post shader chain '{}'", effect.postChain(), ex);
            return null;
        } finally {
            loadingChain = false;
        }
    }

    // looks at the chain JSON first so we can reject 1.21.2+ format packs
    private static boolean isLoadableLegacyFormatChain(PostChainEffect effect) {
        ResourceLocation chainRl = effect.chainResource();
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(chainRl);
        if (res.isEmpty()) {
            Polytone.LOGGER.error("Post shader chain '{}' not found at {}", effect.postChain(), chainRl);
            return false;
        }
        try (Reader reader = res.get().openAsReader()) {
            JsonObject root = GsonHelper.parse(reader);
            //gives a better error message than the default codec parse error
            if (isNewFormatChain(root)) {
                Polytone.LOGGER.error(
                        "Post shader chain '{}' uses the 1.21.2+ post_effect format (fragment_shader / object-shaped targets / UBO-grouped uniforms). " +
                                "Polytone for 1.21.1 only supports the 1.21.1 post-chain format (passes with 'name', flat 'uniforms' array, 'shaders/program/...json' programs). " +
                                "Either downgrade the pack's shaders to the 1.21.1 format or use a polytone build for the matching MC version. Skipping this chain.",
                        effect.postChain());
                return false;
            }
            return true;
        } catch (Exception ex) {
            Polytone.LOGGER.error("Failed to read post shader chain '{}': {}", effect.postChain(), ex.getMessage());
            return false;
        }
    }

    //detects the 1.21.2+ post-effect schema
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

    public void resize(int width, int height) {
        for (PostChain c : activeChains.values()) {
            c.resize(width, height);
        }
        depthSnapshot.resize(width, height);
    }

    public void captureLevelDepthSnapshot() {
        if (anyActiveChainUsesDepth()){
            depthSnapshot.captureLevelDepth();
        }
    }

    public void renderAfterMainPostEffect(float partialTicks) {
        if (activeChains.isEmpty()) return;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        Polytone.POST_TARGETS.ensureAllocated(main.width, main.height);
        if (anyActiveChainUsesDepth()) depthSnapshot.prepareForPostChains();

        for (var entry : activeChains.entrySet()) {
            PostChain chain = entry.getValue();
            processingEffect = entry.getKey();
            try {
                chain.process(partialTicks);
            } catch (Exception e) {
                Polytone.LOGGER.error("Error processing polytone post chain '{}'", chain.getName(), e);
            } finally {
                processingEffect = null;
            }
        }
        main.bindWrite(true);
    }

    public boolean isProcessing() {
        return processingEffect != null;
    }

    //vanilla post chains go through PostPass too, those we leave alone
    public void bindProcessingEffectTo(EffectInstance program) {
        if (processingEffect == null) return;
        processingEffect.bindTo(IShader.ofEffectInstance(program), depthSnapshot.textureId());
    }

    private boolean anyActiveChainUsesDepth() {
        for (PostChainEffect e : activeChains.keySet()) {
            if (e.useDepthBuffer()) return true;
        }
        return false;
    }

    private void closeActiveChains() {
        for (PostChain c : activeChains.values()) {
            c.close();
        }
        activeChains.clear();
    }

    private void closeEverything() {
        closeActiveChains();
        effects.clear();
        failedChains.clear();
        depthSnapshot.close();
    }
}
