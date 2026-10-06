package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.post.PostProgramImports;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL41;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

//vanilla has no compute shaders
public class ComputeProgramInstance implements AutoCloseable {

    private final ResourceLocation path;
    private final Object2IntMap<String> uniformLocations = new Object2IntOpenHashMap<>();
    private int programId = 0;
    private boolean failedToLoad = false;

    public ComputeProgramInstance(ResourceLocation path) {
        this.path = path;
    }

    public boolean ensureLoaded(ResourceManager resources) {
        if (programId == 0 && !failedToLoad) {
            try (InputStream stream = resources.open(path)) {
                String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                programId = GLHelper.createComputeProgram(String.join("", new PostProgramImports().process(source)));
            } catch (Exception e) {
                failedToLoad = true;
                Polytone.LOGGER.error("Failed to load compute shader {}", path, e);
            }
        }
        return programId != 0;
    }

    protected int programId() {
        return programId;
    }

    protected int uniformLocation(String name) {
        return uniformLocations.computeIfAbsent(name, n -> GlStateManager._glGetUniformLocation(programId, name));
    }

    //program doesnt need to be bound for these, DSA
    protected void setUniform(String name, int x) {
        GL41.glProgramUniform1i(programId, uniformLocation(name), x);
    }

    protected void setUniform(String name, float x) {
        GL41.glProgramUniform1f(programId, uniformLocation(name), x);
    }

    protected void setUniform(String name, int x, int y, int z) {
        GL41.glProgramUniform3i(programId, uniformLocation(name), x, y, z);
    }


    @Override
    public void close() {
        failedToLoad = false;
        uniformLocations.clear();
        if (programId == 0) return;
        GlStateManager.glDeleteProgram(programId);
        programId = 0;
    }
}
