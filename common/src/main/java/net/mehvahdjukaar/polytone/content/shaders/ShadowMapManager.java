package net.mehvahdjukaar.polytone.content.shaders;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.SingleFileContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.core.HolderLookup;
import com.google.gson.JsonElement;
import net.minecraft.resources.RegistryOps;

/**
 * Loads the reloadable shadow-map settings (polytone/shadow_map.json) and feeds them to the
 * {@link ShadowMapRenderer}, which does the actual light-POV depth pass. This is the config/reload
 * half of the shadow system; the rendering half lives in ShadowMapRenderer, reached via {@link #renderer()}.
 */
public class ShadowMapManager extends SingleFileContentManager<ShadowMapSettings> {

    private final ShadowMapRenderer renderer = new ShadowMapRenderer();

    // Staged on the (off-thread) parse and pushed to the renderer on apply.
    private ShadowMapSettings parsedSettings = ShadowMapSettings.DEFAULT;

    public ShadowMapManager() {
        super("Shadow Map", "shadow_map.properties", "shadow_map.json", Polytone.MOD_ID);
    }

    public ShadowMapRenderer renderer() {
        return renderer;
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        ShadowMapSettings result = ShadowMapSettings.DEFAULT;
        for (var entry : resources.jsons().entrySet()) { // each pack file overrides only the params it sets; the rest keep defaults
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
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        renderer.setSettings(parsedSettings);
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        this.parsedSettings = ShadowMapSettings.DEFAULT;
        renderer.setSettings(ShadowMapSettings.DEFAULT);
    }
}
