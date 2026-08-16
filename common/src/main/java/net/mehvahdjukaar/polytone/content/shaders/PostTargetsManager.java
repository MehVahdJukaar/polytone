package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.resource.ResourceHandle;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PostTargetsManager extends ContentManager<PostTargetsManager.TargetSpec> {

    public record TargetSpec(Optional<Integer> width, Optional<Integer> height, boolean useDepth) {
        static final SchemaCodec<TargetSpec> CODEC = SchemaRecord.create(TargetSpec.class, i -> i.group(
                i.optional("width", Codec.INT, TargetSpec::width),
                i.optional("height", Codec.INT, TargetSpec::height),
                i.optional("use_depth", Codec.BOOL, false, TargetSpec::useDepth)
        ).apply(i, TargetSpec::new));
    }

    private volatile Map<Identifier, TargetSpec> specs = Map.of();
    private volatile boolean dirty = false;
    private final Map<Identifier, RenderTarget> targets = new HashMap<>();

    public PostTargetsManager() {
        super(Spec.of("Post target", () -> TargetSpec.CODEC)
                .wikiPage("Shaders")
                .folders("post_targets"));
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        Map<Identifier, TargetSpec> parsed = new HashMap<>();
        for (var entry : resources.jsons().entrySet()) {
            TargetSpec.CODEC.parse(ops, entry.getValue())
                    .resultOrPartial(err -> Polytone.LOGGER.error("Failed to parse post target {}: {}", entry.getKey(), err))
                    .ifPresent(spec -> parsed.put(entry.getKey(), spec));
        }
        this.specs = Map.copyOf(parsed);
        this.dirty = true;
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        this.specs = Map.of();
        this.dirty = true;
    }

    public Set<Identifier> allowedTargets() {
        Map<Identifier, TargetSpec> specs = this.specs;
        if (specs.isEmpty()) return LevelTargetBundle.SORTING_TARGETS;
        Set<Identifier> set = new HashSet<>(LevelTargetBundle.SORTING_TARGETS);
        set.addAll(specs.keySet());
        return set;
    }

    // targets without an explicit size follow the frame size
    public void ensureAllocated(int frameWidth, int frameHeight) {
        Map<Identifier, TargetSpec> specs = this.specs;
        if (dirty) {
            destroyAll();
            for (var e : specs.entrySet()) {
                TargetSpec spec = e.getValue();
                targets.put(e.getKey(), new TextureTarget(e.getKey().toString(),
                        spec.width().orElse(frameWidth), spec.height().orElse(frameHeight), spec.useDepth(),
                        GpuFormat.RGBA8_UNORM));
            }
            dirty = false;
        } else {
            for (var e : specs.entrySet()) {
                TargetSpec spec = e.getValue();
                int width = spec.width().orElse(frameWidth);
                int height = spec.height().orElse(frameHeight);
                RenderTarget target = targets.get(e.getKey());
                if (target != null && (target.width != width || target.height != height)) target.resize(width, height);
            }
        }
    }

    public PostChain.TargetBundle wrap(LevelTargetBundle vanilla, FrameGraphBuilder builder) {
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
            ResourceHandle<RenderTarget> handle = custom.get(id);
            return handle != null ? handle : delegate.get(id);
        }

        @Override
        public void replace(Identifier id, ResourceHandle<RenderTarget> handle) {
            if (custom.containsKey(id)) custom.put(id, handle);
            else delegate.replace(id, handle);
        }
    }
}
