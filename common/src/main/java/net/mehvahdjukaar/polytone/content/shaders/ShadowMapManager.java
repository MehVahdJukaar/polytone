package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.SingleJsonOrPropertiesReloadListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class ShadowMapManager extends SingleJsonOrPropertiesReloadListener {

    private final ShadowMapRenderer renderer = new ShadowMapRenderer();

    private ShadowMapSettings parsedSettings = ShadowMapSettings.DEFAULT;

    public ShadowMapManager() {
        super("Shadow Map", "shadow_map.properties", "shadow_map.json", Polytone.MOD_ID);
    }

    public ShadowMapRenderer renderer() {
        return renderer;
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, RegistryAccess access) {
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
