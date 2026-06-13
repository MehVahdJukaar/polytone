package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.mehvahdjukaar.polytone.mixins.accessor.PostPassAccessor;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PostChainEffect {

    public static final Codec<PostChainEffect> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Identifier.CODEC.fieldOf("post_chain").forGetter(p -> p.postChain),
                    ISimpleExp.CODEC.optionalFieldOf("activation_condition", ISimpleExp.ONE).forGetter(p -> p.turnOnCondition),
                    ExpressionUniformBuffers.MAP_CODEC
                            .optionalFieldOf("expression_uniforms", Map.of()).forGetter(p -> p.buffers.expressions())
            ).apply(i, PostChainEffect::new));

    private final Identifier postChain;
    private final ISimpleExp turnOnCondition;
    private final ExpressionUniformBuffers buffers;

    private boolean cachedOn = false;
    private PostChain cachedPostChain = null;
    private final List<Identifier> registeredShaderIds = new ArrayList<>();

    public PostChainEffect(Identifier postChain, ISimpleExp turnOnCondition, Map<String, ISimpleExp> expressionUniforms) {
        this.postChain = postChain;
        this.turnOnCondition = turnOnCondition;
        this.buffers = new ExpressionUniformBuffers(expressionUniforms);
    }

    public ExpressionUniformBuffers buffers() {
        return buffers;
    }

    public void refreshEnabled() {
        cachedOn = turnOnCondition.evaluate() > 0;
    }

    @Nullable
    public PostChain getPostChain(ShaderManager manager) {
        if (!cachedOn) return null;
        if (cachedPostChain == null) {
            try {
                cachedPostChain = manager.getPostChain(postChain, LevelTargetBundle.MAIN_TARGETS);
                buffers.ensureInitialized("Polytone post expr uniform");
                registerByPassShaders(cachedPostChain);
            } catch (Throwable ex) {
                Polytone.LOGGER.error("Failed to load post chain", ex);
                return null;
            }
        }
        if (isPostPassClosed(cachedPostChain)) {
            closeBuffers();
            cachedPostChain = null;
            return null;
        }
        return cachedPostChain;
    }

    public void updateBuffers() {
        buffers.update();
    }

    void closeBuffers() {
        unregisterByPassShaders();
        buffers.close();
    }

    private void registerByPassShaders(PostChain chain) {
        if (buffers.isEmpty()) return;
        for (PostPass pass : chain.passes) {
            Identifier shaderId = ((PostPassAccessor) pass).polytone$getPipeline().getFragmentShader();
            Polytone.CORE_SHADERS.registerExternal(shaderId, buffers);
            registeredShaderIds.add(shaderId);
        }
    }

    private void unregisterByPassShaders() {
        for (Identifier id : registeredShaderIds) {
            Polytone.CORE_SHADERS.unregisterExternal(id, buffers);
        }
        registeredShaderIds.clear();
    }

    private static boolean isPostPassClosed(@Nullable PostChain pass) {
        if (pass != null && !pass.passes.isEmpty()) {
            var buffer = pass.passes.getFirst().infoUbo.buffers[0];
            return buffer.isClosed();
        }
        return false;
    }
}
