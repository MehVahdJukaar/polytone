package net.mehvahdjukaar.polytone.slotify;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class GuiOverlayManager extends JsonPartialReloader {

    private final Map<Gui.HeartType, HeartSprites> heartSprites = new EnumMap<>(Gui.HeartType.class);
    private final Map<ResourceLocation, BlitModifier> blitModifiers = new HashMap<>();

    public GuiOverlayManager() {
        super("overlay_modifiers");
    }

    @Override
    protected void reset() {
        blitModifiers.clear();
    }

    @Override
    protected void process(Map<ResourceLocation, JsonElement> obj, DynamicOps<JsonElement> ops) {
        for (var j : obj.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();

            BlitModifier effect = BlitModifier.CODEC.decode(ops, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode Overlay Modifier with json id " + id + "\n error: " + errorMsg))
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

    public boolean maybeModifyBlit(GuiGraphics gui, Function<ResourceLocation, RenderType> function, TextureAtlasSprite sprite, int x, int y, int offset, int width, int height) {
        if (!active || blitModifiers.isEmpty()) return false;
        var mod = blitModifiers.get(sprite.contents().name());
        if (mod != null) {
            int ind = mod.index();
            if (ind == -1 || ind == index) {
                mod.blitModified(gui, function, sprite, x, x + width, y, y + height, offset);
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


    private void reloadHearths(ResourceManager manager) {
        heartSprites.clear();
        for (var h : Gui.HeartType.values()) {
            if (h != Gui.HeartType.CONTAINER && h != Gui.HeartType.NORMAL) {
                String name = h.name().toLowerCase(Locale.ROOT);
                ResourceLocation fullRes = ResourceLocation.parse("textures/gui/sprites/polytone/heart/container_" + name + "_full.png");
                ResourceLocation halfRes = ResourceLocation.parse("textures/gui/sprites/polytone/heart/container_" + name + "_half.png");
                if (manager.getResource(fullRes).isPresent() && manager.getResource(halfRes).isPresent()) {
                    ResourceLocation fullBlinkingRes = Polytone.res("textures/gui/sprites/polytone/heart/container_" + name + "_full_blinking.png");
                    var fullBlinking = manager.getResource(fullBlinkingRes);
                    ResourceLocation halfBlinkingRes = Polytone.res("textures/gui/sprites/polytone/heart/container_" + name + "_half_blinking.png");
                    var halfBlinking = manager.getResource(halfBlinkingRes);
                    ResourceLocation hardcoreFullRes = Polytone.res("textures/gui/sprites/polytone/heart/container_" + name + "_hardcore_full.png");
                    var hardcoreFull = manager.getResource(hardcoreFullRes);
                    ResourceLocation hardcoreFullBlinkingRes = Polytone.res("textures/gui/sprites/polytone/heart/container_" + name + "_hardcore_full_blinking.png");
                    var hardcoreFullBlinking = manager.getResource(hardcoreFullBlinkingRes);
                    ResourceLocation hardcoreHalfRes = Polytone.res("textures/gui/sprites/polytone/heart/container_" + name + "_hardcore_half.png");
                    var hardcoreHalf = manager.getResource(hardcoreHalfRes);
                    ResourceLocation hardcoreHalfBlinkingRes = Polytone.res("textures/gui/sprites/polytone/heart/container_" + name + "_hardcore_half_blinking.png");
                    var hardcoreHalfBlinking = manager.getResource(hardcoreHalfBlinkingRes);

                    if (fullBlinking.isEmpty()) {
                        fullBlinkingRes = fullRes;
                    }
                    if (halfBlinking.isEmpty()) {
                        halfBlinkingRes = halfRes;
                    }
                    if (hardcoreFull.isEmpty()) {
                        hardcoreFullRes = fullRes;
                    }
                    if (hardcoreFullBlinking.isEmpty()) {
                        hardcoreFullBlinkingRes = hardcoreFullRes;
                    }
                    if (hardcoreHalf.isEmpty()) {
                        hardcoreHalfRes = halfRes;
                    }
                    if (hardcoreHalfBlinking.isEmpty()) {
                        hardcoreHalfBlinkingRes = hardcoreHalfRes;
                    }
                    fullRes = fullRes.withPath(p -> p.replace("textures/gui/sprites/", "").replace(".png", ""));
                    halfRes = halfRes.withPath(p -> p.replace("textures/gui/sprites/", "").replace(".png", ""));
                    fullBlinkingRes = fullBlinkingRes.withPath(p -> p.replace("textures/gui/sprites/", "").replace(".png", ""));
                    halfBlinkingRes = halfBlinkingRes.withPath(p -> p.replace("textures/gui/sprites/", "").replace(".png", ""));
                    hardcoreFullRes = hardcoreFullRes.withPath(p -> p.replace("textures/gui/sprites/", "").replace(".png", ""));
                    hardcoreFullBlinkingRes = hardcoreFullBlinkingRes.withPath(p -> p.replace("textures/gui/sprites/", "").replace(".png", ""));
                    hardcoreHalfRes = hardcoreHalfRes.withPath(p -> p.replace("textures/gui/sprites/", "").replace(".png", ""));
                    hardcoreHalfBlinkingRes = hardcoreHalfBlinkingRes.withPath(p -> p.replace("textures/gui/sprites/", "").replace(".png", ""));
                    heartSprites.put(h, new HeartSprites(fullRes, halfRes, fullBlinkingRes, halfBlinkingRes,
                            hardcoreFullRes, hardcoreHalfRes, hardcoreFullBlinkingRes, hardcoreHalfBlinkingRes));
                }
            }
        }
    }

    public boolean maybeFancifyHeart(Gui instance, GuiGraphics graphics, Gui.HeartType actualType, int i, int j, boolean bl, boolean bl2, boolean bl3) {
        if (heartSprites.isEmpty()) return false;
        HeartSprites sprites = heartSprites.get(actualType);
        if (sprites != null) {
            graphics.blitSprite(RenderType::guiTextured, sprites.getSprite(bl, bl3, bl2), i, j, 9, 9);
            return true;
        }
        return false;
    }


    private record HeartSprites(ResourceLocation full, ResourceLocation half, ResourceLocation fullBlinking,
                                ResourceLocation halfBlinking, ResourceLocation hardcoreFull,
                                ResourceLocation hardcoreHalf, ResourceLocation hardcoreFullBlinking,
                                ResourceLocation hardcoreHalfBlinking) {

        public ResourceLocation getSprite(boolean bl, boolean bl2, boolean bl3) {
            if (!bl) {
                if (bl2) {
                    return bl3 ? this.halfBlinking : this.half;
                } else {
                    return bl3 ? this.fullBlinking : this.full;
                }
            } else if (bl2) {
                return bl3 ? this.hardcoreHalfBlinking : this.hardcoreHalf;
            } else {
                return bl3 ? this.hardcoreFullBlinking : this.hardcoreFull;
            }
        }

    }

}
