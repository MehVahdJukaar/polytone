package net.mehvahdjukaar.polytone.content.packinfo;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public class PackInfoBadge extends Button {

    public static final Identifier SPRITE = Polytone.res("pack_info");
    public static final Identifier SPRITE_HIGHLIGHTED = Polytone.res("pack_info_highlighted");
    public static final int SIZE = 8;

    //dev only, so no lang entry
    private static final Component DEV_TOOLTIP = Component.literal("dummy badge (dev env)");

    private final Supplier<@Nullable PackInfo> info;
    private final Component packName;
    private final Runnable packReload;

    public PackInfoBadge(Supplier<@Nullable PackInfo> info, Component packName, Runnable packReload) {
        super(0, 0, SIZE, SIZE, Component.empty(), b -> {
        }, DEFAULT_NARRATION);
        this.info = info;
        this.packName = packName;
        this.packReload = packReload;
    }

    //in dev we draw it on every pack to check placement, even ones with no info section
    public static boolean shouldShow(@Nullable PackInfo info) {
        return info != null || Polytone.isDevEnv;
    }

    public static Component tooltip(@Nullable PackInfo info) {
        return info == null ? DEV_TOOLTIP : Component.translatable("screen.polytone.pack_info.tooltip");
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        PackInfo info = this.info.get();
        if (!shouldShow(info)) return;

        boolean over = this.isHovered();
        if (over) {
            graphics.setTooltipForNextFrame(tooltip(info), mouseX, mouseY);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, over ? SPRITE_HIGHLIGHTED : SPRITE,
                this.getX(), this.getY(), SIZE, SIZE);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        PackInfo i = this.info.get();
        if (i == null) return;
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new PackInfoScreen(mc.gui.screen(), this.packName, i, this.packReload));
    }
}
