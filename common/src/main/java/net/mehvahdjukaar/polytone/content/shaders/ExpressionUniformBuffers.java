package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.mehvahdjukaar.polytone.mixins.accessor.GlBufferAccessor;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// A map of block name -> expression; each entry becomes a single-float UBO block of that name
public final class ExpressionUniformBuffers {

    public static final Codec<ExpressionUniformBuffers> CODEC =
            Codec.unboundedMap(Codec.STRING, ISimpleExp.CODEC)
                    .xmap(ExpressionUniformBuffers::new, ExpressionUniformBuffers::expressions);

    private static final int FLOAT_UBO_SIZE = new Std140SizeCalculator().putFloat().get();

    private final Map<String, ISimpleExp> expressions;
    private Map<String, GpuBuffer> buffers = null;

    public ExpressionUniformBuffers(Map<String, ISimpleExp> expressions) {
        this.expressions = expressions;
    }

    public boolean isEmpty() {
        return expressions.isEmpty();
    }

    public Map<String, ISimpleExp> expressions() {
        return expressions;
    }

    public void ensureInitialized(String debugLabel) {
        if (buffers == null && !expressions.isEmpty()) {
            buffers = new LinkedHashMap<>();
            for (String name : expressions.keySet()) {
                buffers.put(name, RenderSystem.getDevice().createBuffer(
                        () -> debugLabel + ": " + name,
                        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
                        FLOAT_UBO_SIZE));
            }
        }
    }

    public void update() {
        if (buffers == null) return;
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        for (var e : expressions.entrySet()) {
            GpuBuffer buf = buffers.get(e.getKey());
            if (buf == null) continue;
            float val = (float) e.getValue().evaluate();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer bb = Std140Builder.onStack(stack, FLOAT_UBO_SIZE).putFloat(val).get();
                encoder.writeToBuffer(buf.slice(), bb);
            }
        }
    }

    public void bind(RenderPass pass, Set<String> declaredUniforms) {
        if (buffers == null) return;
        for (var e : buffers.entrySet()) {
            // undeclared binds make Iris/Sodium log errors
            if (declaredUniforms.contains(e.getKey())) {
                pass.setUniform(e.getKey(), e.getValue());
            }
        }
    }

    // raw GL bind for programs not driven through RenderPass (Sodium chunk shaders); only declared blocks are bound
    public int bindBlocksToProgram(int program, int nextBindingPoint) {
        if (buffers == null) return nextBindingPoint;
        for (var e : buffers.entrySet()) {
            int blockIndex = GL32C.glGetUniformBlockIndex(program, e.getKey());
            if (blockIndex < 0) continue;
            int glId = ((GlBufferAccessor) (Object) e.getValue()).polytone$getHandle();
            GL32C.glUniformBlockBinding(program, blockIndex, nextBindingPoint);
            GL30C.glBindBufferBase(GL31C.GL_UNIFORM_BUFFER, nextBindingPoint, glId);
            nextBindingPoint++;
        }
        return nextBindingPoint;
    }

    public void close() {
        if (buffers != null) {
            buffers.values().forEach(GpuBuffer::close);
            buffers = null;
        }
    }
}
