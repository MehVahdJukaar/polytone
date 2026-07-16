package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Persistent render targets declared under {@code polytone/post_targets/<name>.json}
 * ({@code width}/{@code height} optional, defaulting to frame size, plus {@code use_depth}).
 * They keep their content across frames; polytone post effects can sample them by id via the
 * {@code target_samplers} field on a {@link PostChainEffect}.
 *
 * <p><b>1.21.1 port note.</b> The 1.21.11 original spliced these into the vanilla framegraph
 * {@code PostChain.TargetBundle} (via {@code FrameGraphBuilder.importExternal} +
 * {@code LevelTargetBundle.SORTING_TARGETS}) so chains could both READ and WRITE them as first-class
 * framegraph resources. 1.21.1 uses the OLD {@code net.minecraft.client.renderer.PostChain}, which
 * has no framegraph, no {@code TargetBundle}, and manages its own targets internally from the pack
 * JSON - so that write-side splicing has NO equivalent here. This port keeps the allocation +
 * cross-frame persistence and exposes the targets for READ (as samplers). Writing into a persistent
 * target from a chain is not supported on 1.21.1's old PostChain (see gap note in the port report).</p>
 */
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
        super(Spec.of("Post target", () -> SchemaCodec.wrap(TargetSpec.CODEC))
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

    /** Creates or resizes the targets; full-frame ones follow the window size. Call once per frame. */
    public void ensureAllocated(int frameWidth, int frameHeight) {
        Map<ResourceLocation, TargetSpec> specs = this.specs;
        if (dirty) {
            destroyAll();
            for (var e : specs.entrySet()) {
                TargetSpec s = e.getValue();
                targets.put(e.getKey(), new TextureTarget(
                        s.width().orElse(frameWidth), s.height().orElse(frameHeight), s.useDepth(), Minecraft.ON_OSX));
            }
            dirty = false;
        } else {
            for (var e : specs.entrySet()) {
                TargetSpec s = e.getValue();
                int w = s.width().orElse(frameWidth), h = s.height().orElse(frameHeight);
                RenderTarget t = targets.get(e.getKey());
                if (t != null && (t.width != w || t.height != h)) t.resize(w, h, Minecraft.ON_OSX);
            }
        }
    }

    public void close() {
        destroyAll();
    }

    private void destroyAll() {
        for (RenderTarget t : targets.values()) t.destroyBuffers();
        targets.clear();
    }
}
