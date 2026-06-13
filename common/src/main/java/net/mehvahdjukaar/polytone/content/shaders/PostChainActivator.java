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

/**
 * Conditionally adds a post chain to the FrameGraph each frame.
 *
 * <p>{@code expression_uniforms} on this object are chain-level: they get bound to every
 * pass shader in the chain. Implemented internally as a single
 * {@link ExpressionUniformBuffers} registered with {@link ShaderUniformsManager} under each
 * pass's pipeline fragment-shader id. Chain gating ({@link #turnOnCondition}) implicitly
 * gates the binds too — when the chain isn't in the FrameGraph, its passes never bind.
 */
public final class PostChainActivator {

    public static final Codec<PostChainActivator> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Identifier.CODEC.fieldOf("post_chain").forGetter(p -> p.postChain),
                    ISimpleExp.CODEC.optionalFieldOf("activation_condition", ISimpleExp.ONE).forGetter(p -> p.turnOnCondition),
                    ExpressionUniformBuffers.CODEC
                            .optionalFieldOf("expression_uniforms", new ExpressionUniformBuffers(Map.of()))
                            .forGetter(p -> p.buffers)
            ).apply(i, PostChainActivator::new));

    private final Identifier postChain;
    private final ISimpleExp turnOnCondition;
    private final ExpressionUniformBuffers buffers;

    private boolean cachedOn = false;
    private PostChain cachedPostChain = null;
    private final List<Identifier> registeredShaderIds = new ArrayList<>();

    public PostChainActivator(Identifier postChain, ISimpleExp turnOnCondition, ExpressionUniformBuffers buffers) {
        this.postChain = postChain;
        this.turnOnCondition = turnOnCondition;
        this.buffers = buffers;
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
            close();
            return null;
        }
        return cachedPostChain;
    }

    void close() {
        unregisterByPassShaders();
        buffers.close();
        cachedPostChain = null;
    }

    private void registerByPassShaders(PostChain chain) {
        if (buffers.isEmpty()) return;
        for (PostPass pass : chain.passes) {
            Identifier shaderId = ((PostPassAccessor) pass).polytone$getPipeline().getFragmentShader();
            Polytone.SHADER_EFFECTS.registerExternal(shaderId, buffers);
            registeredShaderIds.add(shaderId);
        }
    }

    private void unregisterByPassShaders() {
        for (Identifier id : registeredShaderIds) {
            Polytone.SHADER_EFFECTS.unregisterExternal(id, buffers);
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
