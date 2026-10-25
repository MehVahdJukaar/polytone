package net.mehvahdjukaar.polytone.api;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.config.ConfigsManager;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPassTracker;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneBuiltInUniformsSet;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneRenderHooks;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

//for addons that render their own stuff on top of polytone. call the register methods from mod init
public class PolytoneRenderApi {

    //patchers run in registration order. polytones own colored lights go first
    public static void registerShaderPatcher(IShaderSourcePatch patcher) {
        PolytoneRenderHooks.addSourcePatcher(patcher);
    }

    //binds uniforms and samplers on any shader declaring one of its enabling uniforms, core and post alike
    public static void registerShaderInputPatch(IShaderInputPatch patch) {
        PolytoneRenderHooks.addInputPatch(patch);
    }

    public static void registerLevelRenderListener(LevelRenderListener listener) {
        PolytoneRenderHooks.addLevelRenderListener(listener);
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

    //lands in polytones own config screen as polytone:<name>. lang keys are config.polytone.<name>(.tooltip), section can be null
    public static Supplier<Boolean> registerConfig(String name, boolean defaultValue, @Nullable String section) {
        return Polytone.CONFIGS.addBuiltin(ConfigsManager.builtinConfig(name, section, defaultValue))::get;
    }

    public static Supplier<Float> registerConfig(String name, float defaultValue, float min, float max, float step, @Nullable String section) {
        return Polytone.CONFIGS.addBuiltin(ConfigsManager.builtinConfig(name, section, defaultValue, min, max, step))::get;
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
