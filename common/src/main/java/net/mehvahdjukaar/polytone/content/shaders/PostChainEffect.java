package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PostChainEffect {

    private static final int FLOAT_UBO_SIZE = new Std140SizeCalculator().putFloat().get();

    public static final Codec<PostChainEffect> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Identifier.CODEC.fieldOf("post_chain").forGetter(p -> p.postChain),
                    ISimpleExp.CODEC.optionalFieldOf("activation_condition", ISimpleExp.ONE).forGetter(p -> p.turnOnCondition),
                    Codec.unboundedMap(Codec.STRING, ISimpleExp.CODEC)
                            .optionalFieldOf("expression_uniforms", Map.of()).forGetter(p -> p.expressionUniforms)
            ).apply(i, PostChainEffect::new));

    private final Identifier postChain;
    private final ISimpleExp turnOnCondition;
    private final Map<String, ISimpleExp> expressionUniforms;

    private boolean cachedOn = false;
    private PostChain cachedPostChain = null;
    private Map<String, GpuBuffer> expressionBuffers = null;

    public PostChainEffect(Identifier postChain, ISimpleExp turnOnCondition, Map<String, ISimpleExp> expressionUniforms) {
        this.postChain = postChain;
        this.turnOnCondition = turnOnCondition;
        this.expressionUniforms = expressionUniforms;
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
                initExpressionBuffers();
                Polytone.POST_SHADERS.registerPasses(cachedPostChain, this);
            } catch (Throwable ex) {
                Polytone.LOGGER.error("Failed to load post chain", ex);
                return null;
            }
        }
        if (isPostPassClosed(cachedPostChain)) {
            Polytone.POST_SHADERS.unregisterPasses(cachedPostChain);
            closeExpressionBuffers();
            cachedPostChain = null;
            return null;
        }
        return cachedPostChain;
    }

    void updateExpressionBuffers() {
        if (expressionBuffers == null) return;
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        for (var e : expressionUniforms.entrySet()) {
            GpuBuffer buf = expressionBuffers.get(e.getKey());
            if (buf == null) continue;
            float val = (float) e.getValue().evaluate();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer bb = Std140Builder.onStack(stack, FLOAT_UBO_SIZE).putFloat(val).get();
                encoder.writeToBuffer(buf.slice(), bb);
            }
        }
    }

    public void bindExpressionUniforms(RenderPass pass) {
        if (expressionBuffers == null) return;
        for (var e : expressionBuffers.entrySet()) {
            pass.setUniform(e.getKey(), e.getValue());
        }
    }

    private void initExpressionBuffers() {
        closeExpressionBuffers();
        if (expressionUniforms.isEmpty()) return;
        expressionBuffers = new LinkedHashMap<>();
        for (String name : expressionUniforms.keySet()) {
            expressionBuffers.put(name, RenderSystem.getDevice().createBuffer(
                    () -> "Polytone expr uniform: " + name,
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
                    FLOAT_UBO_SIZE));
        }
    }

    void closeExpressionBuffers() {
        if (expressionBuffers != null) {
            expressionBuffers.values().forEach(GpuBuffer::close);
            expressionBuffers = null;
        }
    }

    private static boolean isPostPassClosed(@Nullable PostChain pass) {
        if (pass != null && !pass.passes.isEmpty()) {
            var buffer = pass.passes.getFirst().infoUbo.buffers[0];
            return buffer.isClosed();
        }
        return false;
    }
}