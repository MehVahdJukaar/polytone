package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.mehvahdjukaar.polytone.mixins.accessor.PostPassAccessor;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PostChainActivator {

    public static final SchemaCodec<PostChainActivator> CODEC = SchemaRecord.create(PostChainActivator.class,
            i -> i.group(
                    i.field("post_chain", Identifier.CODEC, p -> p.postChain),
                    i.optional("activation_condition", ISimpleExp.CODEC, ISimpleExp.ONE, p -> p.turnOnCondition),
                    i.optional("expression_uniforms", ExpressionUniformBuffers.CODEC,
                            new ExpressionUniformBuffers(Map.of()), p -> p.buffers),
                    i.optional("samplers", Codec.unboundedMap(Codec.STRING, Identifier.CODEC),
                            Map.of(), p -> p.samplers),
                    i.optional("use_shadow_map", Codec.BOOL, false, p -> p.useShadowMap)
            ).apply(i, PostChainActivator::new));

    private final Identifier postChain;
    private final ISimpleExp turnOnCondition;
    private final ExpressionUniformBuffers buffers;
    private final Map<String, Identifier> samplers;
    private final boolean useShadowMap;

    private boolean cachedOn = false;
    private PostChain cachedPostChain = null;
    private boolean readsMainDepth = false;
    private final List<Identifier> registeredPassPipelines = new ArrayList<>();

    public PostChainActivator(Identifier postChain, ISimpleExp turnOnCondition,
                              ExpressionUniformBuffers buffers, Map<String, Identifier> samplers,
                              boolean useShadowMap) {
        this.postChain = postChain;
        this.turnOnCondition = turnOnCondition;
        this.buffers = buffers;
        this.samplers = samplers;
        this.useShadowMap = useShadowMap;
    }

    public void refreshEnabled() {
        cachedOn = turnOnCondition.evaluate() > 0;
    }

    // Whether this chain's activation condition currently passes (as of the last refreshEnabled).
    public boolean isOn() {
        return cachedOn;
    }

    // True when this chain is currently on and declared use_shadow_map, i.e. it wants the light-POV depth map
    // rendered this frame (see ShadowMapRenderer).
    public boolean wantsShadowMap() {
        return cachedOn && useShadowMap;
    }

    public boolean readsMainDepth() {
        return cachedOn && readsMainDepth;
    }

    @Nullable
    public PostChain getPostChain(ShaderManager manager) {
        if (!cachedOn) return null;
        if (cachedPostChain == null) {
            try {
                cachedPostChain = manager.getPostChain(postChain, Polytone.POST_TARGETS.allowedTargets());
                if (cachedPostChain == null) return null; // mid-reload, try again next frame
                buffers.ensureInitialized("Polytone post expr uniform");
                readsMainDepth = anyPassReadsMainDepth(cachedPostChain);
                registerOnPasses(cachedPostChain);
            } catch (Throwable ex) {
                Polytone.LOGGER.error("Failed to load post chain", ex);
                return null;
            }
        }
        if (isPostPassClosed(cachedPostChain)) {
            close();
            return null;
        }
        return cachedPostChain;
    }

    void close() {
        unregisterFromPasses();
        buffers.close();
        cachedPostChain = null;
    }

    private void registerOnPasses(PostChain chain) {
        if (buffers.isEmpty() && samplers.isEmpty()) return;
        for (PostPass pass : chain.passes) {
            Identifier pipelineLocation = ((PostPassAccessor) pass).polytone$getPipeline().getLocation();
            if (!buffers.isEmpty()) Polytone.SHADER_EFFECTS.registerOnPostPass(pipelineLocation, buffers);
            if (!samplers.isEmpty()) Polytone.POST_CHAINS.registerSamplers(pipelineLocation, samplers);
            registeredPassPipelines.add(pipelineLocation);
        }
    }

    private void unregisterFromPasses() {
        for (Identifier id : registeredPassPipelines) {
            if (!buffers.isEmpty()) Polytone.SHADER_EFFECTS.unregisterFromPostPass(id, buffers);
            if (!samplers.isEmpty()) Polytone.POST_CHAINS.unregisterSamplers(id, samplers);
        }
        registeredPassPipelines.clear();
    }

    private static boolean anyPassReadsMainDepth(PostChain chain) {
        for (PostPass pass : chain.passes) {
            for (PostPass.Input input : ((PostPassAccessor) pass).polytone$getInputs()) {
                if (input instanceof PostPass.TargetInput t && t.depthBuffer() && t.targetId().equals(PostChain.MAIN_TARGET_ID)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isPostPassClosed(@Nullable PostChain pass) {
        if (pass != null && !pass.passes.isEmpty()) {
            var buffer = pass.passes.getFirst().infoUbo.buffers[0];
            return buffer.isClosed();
        }
        return false;
    }
}
