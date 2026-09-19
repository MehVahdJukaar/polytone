package net.mehvahdjukaar.polytone.content.shaders;

import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.reloader.SingleFileContentManager;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class ShadowMapManager extends SingleFileContentManager<Void> {

    private final ShadowMapRenderer renderer = new ShadowMapRenderer();

    private ShadowMapSettings parsedSettings = ShadowMapSettings.DEFAULT;

    public ShadowMapManager() {
        super("Shadow Map", "shadow_map.properties", "shadow_map.json", Polytone.MOD_ID);
    }

    public ShadowMapRenderer renderer() {
        return renderer;
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        var jsons = resources.jsons();
        ShadowMapSettings result = ShadowMapSettings.DEFAULT;
        for (var entry : jsons.entrySet()) {
            try {
                ShadowMapSettings parsed = ShadowMapSettings.CODEC.parse(ops, entry.getValue()).getOrThrow();
                result = result.merge(parsed);
            } catch (Exception e) {
                Polytone.LOGGER.error("Failed to parse shadow_map.json in file {}", entry.getKey(), e);
            }
        }
        this.parsedSettings = result;
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        renderer.setSettings(parsedSettings);
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        this.parsedSettings = ShadowMapSettings.DEFAULT;
        renderer.setSettings(ShadowMapSettings.DEFAULT);
    }
}
