package net.mehvahdjukaar.polytone.content.shaders.post;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ExtraCodecs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PostTargetsManager extends ContentManager<PostTargetsManager.PostTargetDef> {

    public record PostTargetDef(Optional<Integer> width, Optional<Integer> height, boolean useDepth) {
        public static final SchemaCodec<PostTargetDef> CODEC = SchemaRecord.create(PostTargetDef.class, i -> i.group(
                i.optional("width", ExtraCodecs.POSITIVE_INT, PostTargetDef::width),
                i.optional("height", ExtraCodecs.POSITIVE_INT, PostTargetDef::height),
                i.optional("use_depth", Codec.BOOL, false, PostTargetDef::useDepth)
        ).apply(i, PostTargetDef::new));
    }

    private volatile Map<Identifier, PostTargetDef> definitions = Map.of();
    private volatile boolean dirty = false;
    private final Map<Identifier, RenderTarget> targets = new HashMap<>();

    public PostTargetsManager() {
        super(Spec.of("Post target", () -> PostTargetDef.CODEC)
                .wikiPage("Shaders")
                .folders("post_targets"));
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        Map<Identifier, PostTargetDef> parsed = new HashMap<>();
        for (var entry : resources.jsons().entrySet()) {
            PostTargetDef.CODEC.parse(ops, entry.getValue())
                    .resultOrPartial(err -> Polytone.LOGGER.error("Failed to parse post target {}: {}", entry.getKey(), err))
                    .ifPresent(spec -> parsed.put(entry.getKey(), spec));
        }
        this.definitions = Map.copyOf(parsed);
        this.dirty = true;
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        this.definitions = Map.of();
        destroyAll();
    }

    // Target ids post chains may reference: the vanilla sorting targets plus all custom ones
    public Set<Identifier> allowedTargets() {
        Map<Identifier, PostTargetDef> specs = this.definitions;
        if (specs.isEmpty()) return LevelTargetBundle.SORTING_TARGETS;
        Set<Identifier> set = new HashSet<>(LevelTargetBundle.SORTING_TARGETS);
        set.addAll(specs.keySet());
        return set;
    }

    // Creates or resizes the targets; full-frame ones follow the window size
    public void ensureAllocated(int frameWidth, int frameHeight) {
        Map<Identifier, PostTargetDef> specs = this.definitions;
        if (dirty) {
            destroyAll();
            for (var e : specs.entrySet()) {
                PostTargetDef s = e.getValue();
                RenderTarget target = new TextureTarget(e.getKey().toString(),
                        s.width().orElse(frameWidth), s.height().orElse(frameHeight), s.useDepth());
                clear(target);
                targets.put(e.getKey(), target);
            }
            dirty = false;
        } else {
            for (var e : specs.entrySet()) {
                PostTargetDef s = e.getValue();
                int w = s.width().orElse(frameWidth), h = s.height().orElse(frameHeight);
                RenderTarget t = targets.get(e.getKey());
                if (t != null && (t.width != w || t.height != h)) {
                    t.resize(w, h);
                    clear(t);
                }
            }
        }
    }

    private static void clear(RenderTarget target) {
        GpuTexture color = target.getColorTexture();
        if (color == null) return;
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        GpuTexture depth = target.useDepth ? target.getDepthTexture() : null;
        if (depth != null) encoder.clearColorAndDepthTextures(color, 0, depth, 1.0);
        else encoder.clearColorTexture(color, 0);
    }

    // Wraps the vanilla bundle so custom ids resolve to the persistent targets
    public PostChain.TargetBundle wrap(PostChain.TargetBundle vanilla, FrameGraphBuilder builder) {
        if (targets.isEmpty()) return vanilla;
        Map<Identifier, ResourceHandle<RenderTarget>> handles = new HashMap<>();
        for (var e : targets.entrySet()) {
            handles.put(e.getKey(), builder.importExternal(e.getKey().toString(), e.getValue()));
        }
        return new CustomTargetBundle(vanilla, handles);
    }

    public void close() {
        destroyAll();
    }

    private void destroyAll() {
        for (RenderTarget t : targets.values()) t.destroyBuffers();
        targets.clear();
    }

    private record CustomTargetBundle(PostChain.TargetBundle delegate,
                                      Map<Identifier, ResourceHandle<RenderTarget>> custom)
            implements PostChain.TargetBundle {
        @Override
        public ResourceHandle<RenderTarget> get(Identifier id) {
            ResourceHandle<RenderTarget> h = custom.get(id);
            return h != null ? h : delegate.get(id);
        }

        @Override
        public void replace(Identifier id, ResourceHandle<RenderTarget> handle) {
            if (custom.containsKey(id)) custom.put(id, handle);
            else delegate.replace(id, handle);
        }
    }
}
