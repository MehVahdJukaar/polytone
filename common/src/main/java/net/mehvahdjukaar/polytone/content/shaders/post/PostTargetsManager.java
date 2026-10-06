package net.mehvahdjukaar.polytone.content.shaders.post;

import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PostTargetsManager extends ContentManager<PostTargetsManager.PostTargetDef> {

    public record PostTargetDef(Optional<Integer> width, Optional<Integer> height, boolean useDepth) {
        public static final Codec<PostTargetDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("width").forGetter(PostTargetDef::width),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("height").forGetter(PostTargetDef::height),
                Codec.BOOL.optionalFieldOf("use_depth", false).forGetter(PostTargetDef::useDepth)
        ).apply(i, PostTargetDef::new));
    }

    private final Map<ResourceLocation, RenderTarget> targets = new HashMap<>();
    private volatile Map<ResourceLocation, PostTargetDef> definitions = Map.of();
    private volatile boolean dirty = false;

    public PostTargetsManager() {
        super(Spec.of("Post target", () -> PostTargetDef.CODEC)
                .wikiPage("Shaders")
                .folders("post_targets"));
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        var jsons = resources.jsons();
        Map<ResourceLocation, PostTargetDef> parsed = new HashMap<>();
        for (var entry : jsons.entrySet()) {
            PostTargetDef.CODEC.parse(ops, entry.getValue())
                    .resultOrPartial(err -> Polytone.LOGGER.error("Failed to parse post target {}: {}", entry.getKey(), err))
                    .ifPresent(spec -> parsed.put(entry.getKey(), spec));
        }
        this.definitions = Map.copyOf(parsed);
        this.dirty = true;
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        this.definitions = Map.of();
        destroyAll();
    }

    @Nullable
    public RenderTarget getTarget(ResourceLocation id) {
        return targets.get(id);
    }

    public boolean isEmpty() {
        return definitions.isEmpty();
    }

    public void ensureAllocated(int frameWidth, int frameHeight) {
        Map<ResourceLocation, PostTargetDef> specs = this.definitions;
        if (dirty) {
            destroyAll();
            for (var e : specs.entrySet()) {
                PostTargetDef spec = e.getValue();
                targets.put(e.getKey(), new TextureTarget(
                        spec.width().orElse(frameWidth), spec.height().orElse(frameHeight), spec.useDepth(), Minecraft.ON_OSX));
            }
            dirty = false;
        } else {
            for (var e : specs.entrySet()) {
                PostTargetDef spec = e.getValue();
                int width = spec.width().orElse(frameWidth);
                int height = spec.height().orElse(frameHeight);
                RenderTarget target = targets.get(e.getKey());
                if (target != null && (target.width != width || target.height != height)) {
                    target.resize(width, height, Minecraft.ON_OSX);
                }
            }
        }
    }

    private void destroyAll() {
        for (RenderTarget target : targets.values()) target.destroyBuffers();
        targets.clear();
    }
}
