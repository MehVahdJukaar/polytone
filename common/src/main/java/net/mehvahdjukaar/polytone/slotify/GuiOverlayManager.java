package net.mehvahdjukaar.polytone.slotify;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class GuiOverlayManager {

    private static final Map<Gui.HeartType, HeartSprites> HEART_SPRITES_MAP = new EnumMap<>(Gui.HeartType.class);

    public static void reload(ResourceManager manager) {
        HEART_SPRITES_MAP.clear();
        for (var h : Gui.HeartType.values()) {
            if (h != Gui.HeartType.CONTAINER && h != Gui.HeartType.NORMAL) {
                String name = h.name().toLowerCase(Locale.ROOT);
                ResourceLocation fullRes = new ResourceLocation("textures/gui/sprites/polytone/heart/container_" + name + "_full.png");
                ResourceLocation halfRes = new ResourceLocation("textures/gui/sprites/polytone/heart/container_" + name + "_half.png");
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
                    HEART_SPRITES_MAP.put(h, new HeartSprites(fullRes, halfRes, fullBlinkingRes, halfBlinkingRes,
                            hardcoreFullRes, hardcoreHalfRes, hardcoreFullBlinkingRes, hardcoreHalfBlinkingRes));
                }
            }
        }
    }

    public static boolean maybeFancifyHeart(Gui instance, GuiGraphics graphics, Gui.HeartType actualType, int i, int j, boolean bl, boolean bl2, boolean bl3) {
        if (HEART_SPRITES_MAP.isEmpty()) return false;
        HeartSprites sprites = HEART_SPRITES_MAP.get(actualType);
        if (sprites != null) {
            graphics.blitSprite(sprites.getSprite(bl, bl3, bl2), i, j, 9, 9);
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
