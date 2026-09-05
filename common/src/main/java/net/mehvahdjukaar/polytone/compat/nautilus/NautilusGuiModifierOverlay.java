package net.mehvahdjukaar.polytone.compat.nautilus;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifier;
import net.mehvahdjukaar.polytone.content.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifierManager;
import net.mehvahdjukaar.polytone.content.slotify.ScreenModifier;
import net.mehvahdjukaar.polytone.content.slotify.SlotModifier;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

// One in-memory GuiModifier applied to a single open screen with no resource reload. It always wins for
// the screen it targets and its own condition is ignored while previewing. Touches live screen/menu
// state, so it runs on the render thread; matching is by object identity, so gameplay is unaffected.
public final class NautilusGuiModifierOverlay {

    @Nullable
    private static GuiModifier override;
    @Nullable
    private static Screen screen;
    @Nullable
    private static AbstractContainerMenu menu;

    // in-game picker overlay state, driven by the editor's toggle picking button

    private static boolean pickingEnabled = false;
    @Nullable
    private static Consumer<PickedElement> pickListener;

    public static boolean isPickingEnabled() {
        return pickingEnabled;
    }

    public static void setPickingEnabled(boolean enabled) {
        pickingEnabled = enabled;
    }

    public static void setPickListener(@Nullable Consumer<PickedElement> listener) {
        pickListener = listener;
    }

    public static void onPick(PickedElement picked) {
        Consumer<PickedElement> l = pickListener;
        if (l != null) l.accept(picked);
    }

    // coordinates are relative to screen center, matching target_x/target_y and sprite/text positions;
    // slotIndex is -1 for non-slot picks
    public record PickedElement(int slotIndex, int x, int y, int width, int height, String className) {
    }

    public static boolean isPreviewing(Screen s) {
        return override != null && screen == s;
    }

    public static boolean isPreviewing(AbstractContainerMenu m) {
        return override != null && menu == m;
    }

    @Nullable
    public static GuiModifier override() {
        return override;
    }

    @Nullable
    public static DetectedTarget detectCurrentScreen() {
        return targetOf(Minecraft.getInstance().screen);
    }

    // menu id, else menu class for id-less menus (the survival inventory), else the screen class
    @Nullable
    public static DetectedTarget targetOf(@Nullable Screen s) {
        if (s == null) return null;
        if (s instanceof AbstractContainerScreen<?> cs) {
            AbstractContainerMenu m = cs.getMenu();
            MenuType<?> type = null;
            try {
                type = m.getType();
            } catch (Exception ignored) {
            }
            if (type != null) {
                Identifier id = BuiltInRegistries.MENU.getKey(type);
                if (id != null) return new DetectedTarget(GuiModifier.Type.MENU_ID, id.toString());
            }
            return new DetectedTarget(GuiModifier.Type.MENU_CLASS, m.getClass().getName());
        }
        return new DetectedTarget(GuiModifier.Type.SCREEN_CLASS, s.getClass().getName());
    }

    // re-bakes geometry/widgets/sprites and re-applies slot offsets on the live menu; null clears the
    // preview and restores the screen to its pack-defined state
    public static void pushPreview(@Nullable GuiModifier mod) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> applyOnMainThread(mc, mod));
    }

    private static void applyOnMainThread(Minecraft mc, @Nullable GuiModifier mod) {
        Screen prevScreen = screen;
        AbstractContainerMenu prevMenu = menu;

        Screen open = mc.screen;
        override = mod;
        screen = mod != null ? open : null;
        menu = (mod != null && open instanceof AbstractContainerScreen<?> cs) ? cs.getMenu() : null;

        // When setting, re-apply to the open screen; when clearing, restore the one we were previewing.
        Screen target = open != null ? open : prevScreen;
        if (target instanceof SlotifyScreen ss) {
            ss.polytone$rebuild();          // geometry (idempotent) + widget modifiers
            ss.polytone$refreshModifier();  // per-frame sprites/texts cache
        }
        AbstractContainerMenu targetMenu = target instanceof AbstractContainerScreen<?> cs2 ? cs2.getMenu() : prevMenu;
        if (targetMenu != null) {
            for (Slot slot : targetMenu.slots) {
                Polytone.SLOTIFY.maybeModifySlot(targetMenu, slot);
            }
        }
    }

    public record DetectedTarget(GuiModifier.Type type, String target) {
    }
}
