package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Read only on 1.21.1: the old PostChain has no framegraph to splice a write side into, so unlike
// on 1.21.11 a chain can't write into one.
public class PostTargetsManager extends JsonPartialReloader<PostTargetsManager.TargetSpec> {

    record TargetSpec(Optional<Integer> width, Optional<Integer> height, boolean useDepth) {
        static final Codec<TargetSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("width").forGetter(TargetSpec::width),
                Codec.INT.optionalFieldOf("height").forGetter(TargetSpec::height),
                Codec.BOOL.optionalFieldOf("use_depth", false).forGetter(TargetSpec::useDepth)
        ).apply(i, TargetSpec::new));
    }

    private volatile Map<ResourceLocation, TargetSpec> specs = Map.of();
    private volatile boolean dirty = false;
    private final Map<ResourceLocation, RenderTarget> targets = new HashMap<>();

    public PostTargetsManager() {
        super(Spec.of("Post target", () -> TargetSpec.CODEC)
                .wikiPage("Shaders")
                .folders("post_targets"));
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, RegistryAccess access) {
        Map<ResourceLocation, TargetSpec> parsed = new HashMap<>();
        for (var entry : jsons.entrySet()) {
            TargetSpec.CODEC.parse(ops, entry.getValue())
                    .resultOrPartial(err -> Polytone.LOGGER.error("Failed to parse post target {}: {}", entry.getKey(), err))
                    .ifPresent(spec -> parsed.put(entry.getKey(), spec));
        }
        this.specs = Map.copyOf(parsed);
        this.dirty = true;
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        this.specs = Map.of();
        this.dirty = true;
    }

    @Nullable
    public RenderTarget getTarget(ResourceLocation id) {
        return targets.get(id);
    }

    public boolean isEmpty() {
        return specs.isEmpty();
    }

    // call once per frame; targets without an explicit size follow the frame size
    public void ensureAllocated(int frameWidth, int frameHeight) {
        Map<ResourceLocation, TargetSpec> specs = this.specs;
        if (dirty) {
            destroyAll();
            for (var e : specs.entrySet()) {
                TargetSpec spec = e.getValue();
                targets.put(e.getKey(), new TextureTarget(
                        spec.width().orElse(frameWidth), spec.height().orElse(frameHeight), spec.useDepth(), Minecraft.ON_OSX));
            }
            dirty = false;
        } else {
            for (var e : specs.entrySet()) {
                TargetSpec spec = e.getValue();
                int width = spec.width().orElse(frameWidth);
                int height = spec.height().orElse(frameHeight);
                RenderTarget target = targets.get(e.getKey());
                if (target != null && (target.width != width || target.height != height)) {
                    target.resize(width, height, Minecraft.ON_OSX);
                }
            }
        }
    }

    public void close() {
        destroyAll();
    }

    private void destroyAll() {
        for (RenderTarget target : targets.values()) target.destroyBuffers();
        targets.clear();
    }
}
