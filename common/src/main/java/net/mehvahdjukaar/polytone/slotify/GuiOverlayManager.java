
package net.mehvahdjukaar.polytone.slotify;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GuiOverlayManager extends JsonPartialReloader {

    private final Map<ResourceLocation, BlitModifier> blitModifiers = new HashMap<>();

    public GuiOverlayManager() {
        super("overlay_modifiers");
    }

    @Override
    protected void reset() {
        heartSprites.clear();
        blitModifiers.clear();
    }

    @Override
    protected void process(Map<ResourceLocation, JsonElement> obj) {
        for (var j : obj.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            BlitModifier effect = BlitModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Overlay Modifier with json id {} - error: {}", id, errorMsg))
                    .getFirst();

            ResourceLocation textureId = effect.target();
            if (blitModifiers.containsKey(textureId)) {
                Polytone.LOGGER.warn("Overlay Modifier with texture id {} already exists. Overwriting", textureId);
            }
            blitModifiers.put(textureId, effect);
        }
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager) {
        reloadHearths(resourceManager);
        return super.prepare(resourceManager);
    }

    private int index = 0;
    private boolean active = false;

    public boolean maybeModifyBlit(GuiGraphics gui, TextureAtlasSprite sprite, int x, int y, int offset, int width, int height) {
        if (!active || blitModifiers.isEmpty()) return false;
        var mod = blitModifiers.get(sprite.contents().name());
        if (mod != null) {
            int ind = mod.index();
            if (ind == -1 || ind == index) {
                mod.blitModified(gui, sprite.atlasLocation(), x, x + width, y, y + height, offset,
                        sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
                return true;
            }
            index++;
        }
        return false;
    }

    public void onStartRenderingOverlay() {
        index = 0;
        active = true;
    }

    public void onEndRenderingOverlay() {
        active = false;
    }

}