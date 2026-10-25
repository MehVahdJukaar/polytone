package net.mehvahdjukaar.polytone.api;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPassTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

//for addons that render their own stuff on top of polytone. call the register methods from mod init
public class PolytoneRenderApi {

    //patchers run in registration order. polytones own colored lights go first
    public static void registerShaderPatcher(IShaderSourcePatcher patcher) {
        Polytone.SHADER_PATCHERS.add(patcher);
    }

    //binds uniforms and samplers on any shader declaring one of its enabling uniforms, core and post alike
    public static void registerShaderInputPatch(IShaderInputPatch patch) {
        Polytone.SHADER_INPUT_PATCHES.add(patch);
    }

    public static void registerLevelRenderListener(LevelRenderListener listener) {
        Polytone.LEVEL_RENDER_LISTENERS.add(listener);
    }

    public static void registerContentManager(ContentManager<?> manager) {
        Polytone.registerContentManager(manager);
    }

    public static void registerCoreShader(ResourceLocation id, VertexFormat format, Consumer<ShaderInstance> onLoad) {
        PlatStuff.registerShaders(id, format, onLoad);
    }

    // the view being rendered right now. differs from the main camera inside extra passes
    public static Camera currentCamera() {
        return LevelRenderPassTracker.currentCamera();
    }


    @Nullable
    public static ClientLevel currentLevel() {
        return LevelRenderPassTracker.currentLevel();
    }

    public static boolean isInLevelPass() {
        return LevelRenderPassTracker.isInLevelPass();
    }

    //iris replaces most of the pipeline when this is on. better to step aside
    public static boolean isIrisShaderPackActive() {
        return CompatHandler.irisShaderPackActive();
    }
}
