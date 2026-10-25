package net.mehvahdjukaar.polytone.api;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPassTracker;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneBuiltInUniformsSet;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

//for addons that render their own stuff on top of polytone. call the register methods from mod init
public class PolytoneRenderApi {

    //patchers run in registration order. polytones own colored lights go first
    public static void registerShaderPatcher(IShaderSourcePatcher patcher) {
        Polytone.SHADER_PATCHERS.add(patcher);
    }

    //binds uniforms and samplers on any shader declaring one of its enabling uniforms, core and post alike
    public static void registerShaderInputPatch(IShaderInputPatch patch) {
        Polytone.SHADER_INPUT_PATCHES.add(patch);
        IShaderInputPatch.invalidateEnabledLists();
    }

    public static void registerLevelRenderListener(LevelRenderListener listener) {
        Polytone.LEVEL_RENDER_LISTENERS.add(listener);
    }

    public static void registerContentManager(ContentManager<?> manager) {
        Polytone.registerContentManager(manager);
    }

    //lets core shaders declare this UBO without every pipeline listing it
    public static void registerUniformBlock(String blockName) {
        PolytoneBuiltInUniformsSet.register(blockName);
    }

    public static void registerSampler(String samplerName) {
        PolytoneBuiltInUniformsSet.registerSampler(samplerName);
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
