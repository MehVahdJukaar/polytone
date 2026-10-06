package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.mehvahdjukaar.polytone.Polytone;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;

import java.nio.ByteBuffer;

//all gl directives go here
//4.5 is fun too bad for MAC lol
public class GLHelper {

    private static int @Nullable [] contextVersion;

    //no filtering anywhere, the shaders want exact texels
    public static int create2d(int width, int height, int internalFormat, int format, int type, @Nullable ByteBuffer pixels) {
        int id = GlStateManager._genTexture();
        // through GlStateManager so vanilla's cached binding stays correct
        GlStateManager._bindTexture(id);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        setupUnpackState();
        //format and type say what the bytes are, the buffer type doesnt matter
        GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height,
                0, format, type, pixels == null ? null : pixels.asIntBuffer());
        return id;
    }

    //using DSA since we can. vanilla has nothing for 3d textures
    public static int create3d(int width, int height, int depth, int internalFormat, int format, int type, @Nullable ByteBuffer pixels) {
        int id = GL45.glCreateTextures(GL12.GL_TEXTURE_3D);
        GL45.glTextureParameteri(id, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL45.glTextureParameteri(id, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL45.glTextureStorage3D(id, 1, internalFormat, width, height, depth);
        if (pixels != null) update3d(id, 0, 0, 0, width, height, depth, format, type, pixels);
        return id;
    }

    //linear so one texture() call does the trilinear blend, repeat for the wrapped ring
    public static int createRingBuffer3d(int width, int height, int depth, int internalFormat, int format, int type, ByteBuffer zeros) {
        int id = GL45.glCreateTextures(GL12.GL_TEXTURE_3D);
        GL45.glTextureParameteri(id, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL45.glTextureParameteri(id, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL45.glTextureParameteri(id, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL45.glTextureParameteri(id, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL45.glTextureParameteri(id, GL12.GL_TEXTURE_WRAP_R, GL11.GL_REPEAT);
        GL45.glTextureStorage3D(id, 1, internalFormat, width, height, depth);
        update3d(id, 0, 0, 0, width, height, depth, format, type, zeros);
        return id;
    }

    //DSA so vanillas active texture is never touched
    public static void bindTextureUnit(int unit, int textureId) {
        GL45.glBindTextureUnit(unit, textureId);
    }

    //these run with and without DSA / GL 4.5 so we must ols OldenGl
    public static void setProgramUniform(int program, String name, float x, float y, float z) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location == -1) return;
        GlStateManager._glUseProgram(program);
        GL20.glUniform3f(location, x, y, z);
    }

    public static void setProgramUniform(int program, String name, int x) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location == -1) return;
        GlStateManager._glUseProgram(program);
        GL20.glUniform1i(location, x);
    }

    public static void setProgramUniform(int program, String name, int x, int y, int z) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location == -1) return;
        GlStateManager._glUseProgram(program);
        GL20.glUniform3i(location, x, y, z);
    }

    public static void setProgramUniform4fv(int program, String name, float[] values) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location == -1) return;
        GlStateManager._glUseProgram(program);
        GL20.glUniform4fv(location, values);
    }

    public static boolean usesUniform(int program, String name) {
        return GL20.glGetUniformLocation(program, name) != -1;
    }

    public static void update2d(int id, int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
        setupUnpackState();
        GL45.glTextureSubImage2D(id, 0, x, y, width, height, format, type, pixels);
    }

    public static void update3d(int id, int x, int y, int z, int width, int height, int depth,
                         int format, int type, ByteBuffer pixels) {
        setupUnpackState();
        GL45.glTextureSubImage3D(id, 0, x, y, z, width, height, depth, format, type, pixels);
    }

    //for depth only targets
    public static void disableColorAttachment(RenderTarget target) {
        int previous = GlStateManager.getBoundFramebuffer();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.frameBufferId);
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previous);
    }

    //one call does create, compile, link and drops the shader object. compile errors end up in the program log
    public static int createComputeProgram(String source) {
        //not using gl state manager. since we can use gl41 and DSA we do as it makes this code much smaller. this creates shader & program in 1 go
        int program = GL41.glCreateShaderProgramv(GL43.GL_COMPUTE_SHADER, source);
        if (GlStateManager.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GlStateManager.glGetProgramInfoLog(program, 32768);
            GlStateManager.glDeleteProgram(program);
            throw new IllegalStateException("Failed to build compute shader: " + log);
        }
        return program;
    }

    //Rip Mac lmao
    public static boolean supportsComputeShadersAndDSA() {
        var caps = GL.getCapabilities();
        return caps.OpenGL45 || (caps.GL_ARB_direct_state_access && caps.GL_ARB_compute_shader &&
                caps.GL_ARB_shader_image_load_store && caps.GL_ARB_separate_shader_objects);
    }

    public static boolean supportsOpenGL(int major, int minor) {
        //a 4.3 driver with the DSA extensionis also ok
        boolean extensionsCanCoverIt = major < 4 || (major == 4 && minor <= 5);
        if (extensionsCanCoverIt && supportsComputeShadersAndDSA()) return true;
        int[] have = contextVersion();
        return have[0] > major || (have[0] == major && have[1] >= minor);
    }

    public static String getContextVersionStr() {
        int[] have = contextVersion();
        return have[0] + "." + have[1];
    }

    private static int[] contextVersion() {
        if (contextVersion == null) {
            contextVersion = new int[]{GL11.glGetInteger(GL30.GL_MAJOR_VERSION), GL11.glGetInteger(GL30.GL_MINOR_VERSION)};
        }
        return contextVersion;
    }

    public static String getContextDescription() {
        return GlStateManager._getString(GL11.GL_VERSION) + " on " + GlStateManager._getString(GL11.GL_RENDERER);
    }

    public static void safeDeleteTexture(int id) {
        if (id != 0) GlStateManager._deleteTexture(id);
    }

    private static void setupUnpackState() {
        //how gpu unpacks the buffer we send
        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL12.GL_UNPACK_IMAGE_HEIGHT, 0);
        GlStateManager._pixelStore(GL12.GL_UNPACK_SKIP_IMAGES, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 1);
    }
}
