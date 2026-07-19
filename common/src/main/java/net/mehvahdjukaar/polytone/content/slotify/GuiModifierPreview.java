package net.mehvahdjukaar.polytone.content.slotify;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Client-side bridge for the pack editor's live GUI-modifier preview.
 *
 * <p>Holds one in-memory override {@link GuiModifier} that is applied to a single open screen with no
 * resource reload, so edits in the editor show on the real game screen instantly. The override always
 * wins for the screen it targets and is applied <b>unconditionally</b> - its {@code condition} is
 * ignored while previewing, so the author always sees the result they are editing.
 *
 * <p>Everything here touches live screen/menu state and must run on the render thread; {@link #pushPreview}
 * marshals itself there. Matching is by object identity against the currently previewed screen/menu, so
 * normal gameplay (no override set) is never affected.
 */
public final class GuiModifierPreview {

    @Nullable
    private static GuiModifier override;
    @Nullable
    private static Screen screen;
    @Nullable
    private static AbstractContainerMenu menu;

    // ---- in-game picker overlay state (driven by the editor's "toggle picking" button) -------------

    private static boolean pickingEnabled = false;
    @Nullable
    private static Consumer<PickedElement> pickListener;

    public static boolean isPickingEnabled() {
        return pickingEnabled;
    }

    public static void setPickingEnabled(boolean enabled) {
        pickingEnabled = enabled;
    }

    /** The editor registers a listener here; the in-game overlay fires it when an element is clicked. */
    public static void setPickListener(@Nullable Consumer<PickedElement> listener) {
        pickListener = listener;
    }

    /** Called by the overlay (render/input thread) when the user clicks an element while picking. */
    public static void onPick(PickedElement picked) {
        Consumer<PickedElement> l = pickListener;
        if (l != null) l.accept(picked);
    }

    /**
     * One picked slot/element, in the coordinate space the modifier JSON uses (relative to screen
     * center, matching {@code target_x}/{@code target_y} and sprite/text positions). {@code slotIndex}
     * is -1 for non-slot picks.
     */
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

    /**
     * The target descriptor of the currently open screen, for the editor's "select screen" button.
     * Prefers a menu id, falls back to the menu class (id-less menus like the survival inventory),
     * then the screen class for non-container screens.
     */
    @Nullable
    public static DetectedTarget detectCurrentScreen() {
        return targetOf(Minecraft.getInstance().screen);
    }

    /**
     * The target descriptor a modifier would use to match this screen: menu id, else menu class for
     * id-less menus, else the screen class. Null for no screen. Shared by the editor's detect button
     * and the in-game overlay's targeting readout.
     */
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
                ResourceLocation id = BuiltInRegistries.MENU.getKey(type);
                if (id != null) return new DetectedTarget(GuiModifier.Type.MENU_ID, id.toString());
            }
            return new DetectedTarget(GuiModifier.Type.MENU_CLASS, m.getClass().getName());
        }
        return new DetectedTarget(GuiModifier.Type.SCREEN_CLASS, s.getClass().getName());
    }

    /**
     * Push an in-memory modifier onto the currently open screen (marshaled to the render thread).
     * Re-bakes geometry/widgets/sprites and re-applies slot offsets on the live menu. Pass {@code null}
     * to clear the preview and restore the screen to its pack-defined (or vanilla) state.
     */
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
