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

// One polytone/post_chains json: a vanilla post chain plus the condition that turns it on. Its expression
// uniforms and samplers are registered under every pass's fragment shader id so they reach all passes.
public final class PostChainActivator {

    public static final SchemaCodec<PostChainActivator> CODEC = SchemaRecord.create(PostChainActivator.class,
            i -> i.group(
                    i.field("post_chain", Identifier.CODEC, p -> p.postChainId),
                    i.optional("activation_condition", ISimpleExp.CODEC, ISimpleExp.ONE, p -> p.activationCondition),
                    i.optional("expression_uniforms", ExpressionUniformBuffers.CODEC,
                            new ExpressionUniformBuffers(Map.of()), p -> p.expressionUniforms),
                    i.optional("samplers", Codec.unboundedMap(Codec.STRING, Identifier.CODEC),
                            Map.of(), p -> p.samplers),
                    i.optional("use_shadow_map", Codec.BOOL, false, p -> p.useShadowMap)
            ).apply(i, PostChainActivator::new));

    private final Identifier postChainId;
    private final ISimpleExp activationCondition;
    private final ExpressionUniformBuffers expressionUniforms;
    private final Map<String, Identifier> samplers;
    private final boolean useShadowMap;

    private boolean active = false;
    private PostChain cachedPostChain = null;
    private final List<Identifier> registeredPassShaderIds = new ArrayList<>();

    public PostChainActivator(Identifier postChainId, ISimpleExp activationCondition,
                              ExpressionUniformBuffers expressionUniforms, Map<String, Identifier> samplers,
                              boolean useShadowMap) {
        this.postChainId = postChainId;
        this.activationCondition = activationCondition;
        this.expressionUniforms = expressionUniforms;
        this.samplers = samplers;
        this.useShadowMap = useShadowMap;
    }

    public void refreshActive() {
        active = activationCondition.evaluate() > 0;
    }

    public boolean isActive() {
        return active;
    }

    public boolean wantsShadowMap() {
        return active && useShadowMap;
    }

    @Nullable
    public PostChain getPostChain(ShaderManager manager) {
        if (!active) return null;
        if (cachedPostChain == null) {
            try {
                cachedPostChain = manager.getPostChain(postChainId, Polytone.POST_TARGETS.allowedTargets());
                if (cachedPostChain == null) return null; // mid-reload, try again next frame
                expressionUniforms.ensureInitialized("Polytone post expr uniform");
                registerOnPassShaders(cachedPostChain);
            } catch (Throwable ex) {
                Polytone.LOGGER.error("Failed to load post chain", ex);
                return null;
            }
        }
        if (isChainClosed(cachedPostChain)) {
            close();
            return null;
        }
        return cachedPostChain;
    }

    void close() {
        unregisterFromPassShaders();
        expressionUniforms.close();
        cachedPostChain = null;
    }

    private void registerOnPassShaders(PostChain chain) {
        if (expressionUniforms.isEmpty() && samplers.isEmpty()) return;
        for (PostPass pass : chain.passes) {
            Identifier shaderId = ((PostPassAccessor) pass).polytone$getPipeline().getFragmentShader();
            if (!expressionUniforms.isEmpty()) Polytone.SHADER_EFFECTS.registerExternal(shaderId, expressionUniforms);
            if (!samplers.isEmpty()) Polytone.POST_CHAINS.registerSamplers(shaderId, samplers);
            registeredPassShaderIds.add(shaderId);
        }
    }

    private void unregisterFromPassShaders() {
        for (Identifier id : registeredPassShaderIds) {
            if (!expressionUniforms.isEmpty()) Polytone.SHADER_EFFECTS.unregisterExternal(id, expressionUniforms);
            if (!samplers.isEmpty()) Polytone.POST_CHAINS.unregisterSamplers(id, samplers);
        }
        registeredPassShaderIds.clear();
    }

    // The ShaderManager closes the chain's buffers on reload; a closed first pass means our cached chain is dead
    private static boolean isChainClosed(@Nullable PostChain chain) {
        if (chain != null && !chain.passes.isEmpty()) {
            return chain.passes.getFirst().infoUbo.buffers[0].isClosed();
        }
        return false;
    }
}
