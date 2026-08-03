package net.mehvahdjukaar.polytone.content.slotify;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GuiModifierManager extends ContentManager<GuiModifier> {

    //value modifiers
    private final Map<MenuType<?>, Set<SlotModifier>> slotsByMenuId = new IdentityHashMap<>();
    private final Map<Class<?>, Set<SlotModifier>> slotsByClass = new IdentityHashMap<>();
    private final Map<String, Set<SlotModifier>> slotsByTitle = new HashMap<>();

    //screen modifiers. Lists (not merged at parse) so per-variant conditions survive; they are
    //filtered by condition and merged together at lookup time in resolve(...)
    public final Map<MenuType<?>, List<ScreenModifier>> byMenuId = new IdentityHashMap<>();
    public final Map<Class<?>, List<ScreenModifier>> byClass = new IdentityHashMap<>();
    public final Map<String, List<ScreenModifier>> byTitle = new HashMap<>();


    private static final Identifier INVENTORY = Identifier.parse("inventory");

    public GuiModifierManager() {
        super(Spec.of("GUI modifier", () -> SchemaCodec.wrap(GuiModifier.CODEC))
                .wikiPage("Gui-Modifiers")
                .folders("gui_modifiers"));
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        slotsByMenuId.clear();
        slotsByClass.clear();
        slotsByTitle.clear();
        byMenuId.clear();
        byClass.clear();
        byTitle.clear();
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        Map<Identifier, JsonElement> jsons = resources.jsons();
        List<GuiModifier> allModifiers = new ArrayList<>();

        for (var entry : parseEnabledJsons(jsons, ops)) {
            allModifiers.add(entry.getValue());
        }

        for (GuiModifier mod : allModifiers) {
            //inventory has a null menu type for some reason
            if (mod.targetsClass()) {
                String target = mod.target();
                try {
                    Class<?> cl;
                    if (target.equals("InventoryMenu")) {
                        cl = InventoryMenu.class;
                    } else if (target.equals("ItemPickerMenu")) {
                        cl = CreativeModeInventoryScreen.ItemPickerMenu.class;
                    } else cl = Class.forName(target);
                    byClass.computeIfAbsent(cl, k -> new ArrayList<>()).add(ScreenModifier.fromGuiMod(mod));

                    if (!mod.slotModifiers().isEmpty()) {
                        Set<SlotModifier> map = slotsByClass.computeIfAbsent(cl,
                                i -> new HashSet<>());
                        map.addAll(mod.slotModifiers());
                    }

                } catch (ClassNotFoundException ignored) {
                    Polytone.LOGGER.error("Could not find class target with name {}", target);
                }


            } else if (mod.targetsMenuId()) {
                Identifier menuId = Identifier.parse(mod.target());
                boolean isInventory = menuId.equals(INVENTORY);
                Optional<MenuType<?>> menu = BuiltInRegistries.MENU.getOptional(menuId);

                if (menu.isPresent() || isInventory) {
                    byMenuId.computeIfAbsent(menu.orElse(null), k -> new ArrayList<>()).add(ScreenModifier.fromGuiMod(mod));

                    if (!mod.slotModifiers().isEmpty()) {
                        Set<SlotModifier> map = slotsByMenuId.computeIfAbsent(menu.orElse(null),
                                i -> new HashSet<>());
                        map.addAll(mod.slotModifiers());
                    }
                }
            } else {
                //title target
                String title = mod.target();
                byTitle.computeIfAbsent(title, k -> new ArrayList<>()).add(ScreenModifier.fromGuiMod(mod));

                if (!mod.slotModifiers().isEmpty()) {
                    Set<SlotModifier> map = slotsByTitle.computeIfAbsent(title,
                            i -> new HashSet<>());
                    map.addAll(mod.slotModifiers());
                }
            }

        }

    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        if (!slotsByMenuId.isEmpty() || !slotsByClass.isEmpty() || !slotsByTitle.isEmpty()) {
            Polytone.LOGGER.info("Loaded GUI modifiers for: {} {} {} {}", slotsByMenuId.keySet(), slotsByClass.keySet(), byMenuId.keySet(), byClass.keySet());
        }
    }

    /** Keeps only the candidates whose condition currently passes, then merges them (file order). */
    @Nullable
    private static ScreenModifier resolve(@Nullable List<ScreenModifier> candidates) {
        if (candidates == null) return null;
        ScreenModifier acc = null;
        for (ScreenModifier m : candidates) {
            if (m.passesCondition()) {
                acc = acc == null ? m : acc.merge(m);
            }
        }
        return acc;
    }

    private ScreenModifier getScreenModifier(AbstractContainerScreen<?> screen) {
        ScreenModifier m = null;
        AbstractContainerMenu menu = screen.getMenu();
        if (screen.getClass() == InventoryScreen.class) {
            m = resolve(byClass.get(InventoryMenu.class));
        } else if (screen.getClass() == CreativeModeInventoryScreen.class) {
            m = resolve(byClass.get(CreativeModeInventoryScreen.ItemPickerMenu.class));
        }
        if (menu != null) {
            m = resolve(byClass.get(menu.getClass()));
        }
        if (m == null) {
            MenuType<?> type;
            try {
                type = menu.getType();
            } catch (Exception e) {
                //null for inventory?
                type = null;
            }
            m = resolve(byMenuId.get(type));
        }
        return m;
    }

    @Nullable
    public ScreenModifier getGuiModifier(Screen screen) {
        // Live editor preview wins for the screen it targets, applied unconditionally.
        if (GuiModifierPreview.isPreviewing(screen)) {
            GuiModifier o = GuiModifierPreview.override();
            if (o != null) return ScreenModifier.fromGuiMod(o);
        }
        ScreenModifier m = resolve(byClass.get(screen.getClass()));
        if (m == null && screen instanceof AbstractContainerScreen<?> as) {
            m = getScreenModifier(as);
        }
        Component c;
        try {
            c = screen.getTitle();
        } catch (Exception e) {
            return null;
        }
        if (m == null) {
            m = resolve(byTitle.get(c.getString()));
        }
        if (m == null && c instanceof MutableComponent mc && mc.getContents() instanceof TranslatableContents tc) {
            m = resolve(byTitle.get(tc.getKey()));
        }
        return m;
    }

    //TODO: add back?? why is this commented out
    public Collection<SlotModifier> getSlotModifiers(AbstractContainerScreen<?> screen, Slot slot) {
        if (GuiModifierPreview.isPreviewing(screen)) return previewSlotModifiers(slot);
        Set<SlotModifier> modifies;
        var c = screen.getTitle();
        modifies = slotsByTitle.get(c.getString());
        if (modifies == null && c instanceof MutableComponent mc && mc.getContents() instanceof TranslatableContents tc) {
            modifies = slotsByTitle.get(tc.getKey());
        }
        if (modifies == null) {
            modifies = slotsByClass.get(screen.getClass());
        }
        if (modifies == null) slotsByClass.get(screen.getMenu().getClass());
        if (modifies == null) {
            MenuType<?> type;
            try {
                type = screen.getMenu().getType();
            } catch (Exception e) {
                type = null;
            }
            modifies = slotsByMenuId.get(type);
        }
        if (modifies != null) {

            return modifies.stream().filter(m -> m.matches(slot)).toList();
        }
        return Set.of();
    }

    public Collection<SlotModifier> getSlotModifiers(AbstractContainerMenu menu, Slot slot) {
        if (GuiModifierPreview.isPreviewing(menu)) return previewSlotModifiers(slot);
        var modifiers = slotsByClass.get(menu.getClass());
        if (modifiers == null) {
            MenuType<?> type;
            try {
                type = menu.getType();
            } catch (Exception e) {
                type = null;
            }
            modifiers = slotsByMenuId.get(type);
        }
        if (modifiers != null) {
            return modifiers.stream().filter(m -> m.matches(slot)).toList();
        }
        return Set.of();
    }


    // Idempotent: snapshots the slot's pristine position on first call, resets to it before each apply,
    // so it can be re-run on an already-built menu (live editor preview) without drifting.
    public void maybeModifySlot(AbstractContainerMenu menu, Slot slot) {
        if (slot instanceof SlotifySlot ss) {
            ss.polytone$captureBase();
            ss.polytone$resetToBase();
        }
        var mods = getSlotModifiers(menu, slot);
        for (SlotModifier mod : mods) {
            if (mod.matches(slot)) {
                mod.modify(slot);
            }
        }
    }

    /** Preview override slot modifiers matching this slot, or empty when no override is active. */
    private static Collection<SlotModifier> previewSlotModifiers(Slot slot) {
        GuiModifier o = GuiModifierPreview.override();
        if (o == null) return Set.of();
        return o.slotModifiers().stream().filter(m -> m.matches(slot)).toList();
    }

}
