package net.mehvahdjukaar.polytone.slotify;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.Parsed;
import net.minecraft.client.gui.GuiGraphics;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GuiModifierManager extends JsonPartialReloader {

    //value modifiers
    private final Map<MenuType<?>, Set<SlotModifier>> slotsByMenuId = new IdentityHashMap<>();
    private final Map<Class<?>, Set<SlotModifier>> slotsByClass = new IdentityHashMap<>();
    private final Map<String, Set<SlotModifier>> slotsByTitle = new HashMap<>();

    //screen modifiers
    public final Map<MenuType<?>, ScreenModifier> byMenuId = new IdentityHashMap<>();
    public final Map<Class<?>, ScreenModifier> byClass = new IdentityHashMap<>();
    public final Map<String, ScreenModifier> byTitle = new HashMap<>();


    private static final ResourceLocation INVENTORY = ResourceLocation.parse("inventory");

    public GuiModifierManager() {
        super("gui_modifiers");
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
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        List<GuiModifier> allModifiers = new ArrayList<>();

        for (var entry : jsons.entrySet()) {
            var json = entry.getValue();
            var id = entry.getKey();
            GuiModifier modifier = Parsed.parseOrNull(GuiModifier.CODEC, json, ops, id, "gui modifier");
            if (modifier != null) allModifiers.add(modifier);
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
                    byClass.merge(cl, ScreenModifier.fromGuiMod(mod), ScreenModifier::merge);

                    if (!mod.slotModifiers().isEmpty()) {
                        Set<SlotModifier> map = slotsByClass.computeIfAbsent(cl,
                                i -> new HashSet<>());
                        map.addAll(mod.slotModifiers());
                    }

                } catch (ClassNotFoundException ignored) {
                    Polytone.LOGGER.error("Could not find class target with name {}", target);
                }


            } else if (mod.targetsMenuId()) {
                ResourceLocation menuId = ResourceLocation.parse(mod.target());
                boolean isInventory = menuId.equals(INVENTORY);
                Optional<MenuType<?>> menu = BuiltInRegistries.MENU.getOptional(menuId);

                if (menu.isPresent() || isInventory) {
                    byMenuId.merge(menu.orElse(null), ScreenModifier.fromGuiMod(mod), ScreenModifier::merge);

                    if (!mod.slotModifiers().isEmpty()) {
                        Set<SlotModifier> map = slotsByMenuId.computeIfAbsent(menu.orElse(null),
                                i -> new HashSet<>());
                        map.addAll(mod.slotModifiers());
                    }
                }
            } else {
                //title target
                String title = mod.target();
                byTitle.merge(title, ScreenModifier.fromGuiMod(mod), ScreenModifier::merge);

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

    private ScreenModifier getScreenModifier(AbstractContainerScreen<?> screen) {
        ScreenModifier m = null;
        AbstractContainerMenu menu = screen.getMenu();
        if (screen.getClass() == InventoryScreen.class) {
            m = byClass.get(InventoryMenu.class);
        } else if (screen.getClass() == CreativeModeInventoryScreen.class) {
            m = byClass.get(CreativeModeInventoryScreen.ItemPickerMenu.class);
        }
        if (menu != null) {
            m = byClass.get(menu.getClass());
        }
        if (m == null) {
            MenuType<?> type;
            try {
                type = menu.getType();
            } catch (Exception e) {
                type = null;
            }
            m = byMenuId.get(type);
        }
        return m;
    }

    @Nullable
    public ScreenModifier getGuiModifier(Screen screen) {
        ScreenModifier m = byClass.get(screen.getClass());
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
            m = byTitle.get(c.getString());
        }
        if (m == null && c instanceof MutableComponent mc && mc.getContents() instanceof TranslatableContents tc) {
            m = byTitle.get(tc.getKey());
        }
        return m;
    }

    public Collection<SlotModifier> getSlotModifiers(AbstractContainerScreen<?> screen, Slot slot) {
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


    public void maybeModifySlot(AbstractContainerMenu menu, Slot slot) {
        var mods = getSlotModifiers(menu, slot);
        for (SlotModifier mod : mods) {
            if (mod.matches(slot)) {
                mod.modify(slot);
            }
        }
    }

    public boolean maybeChangeColor(AbstractContainerScreen<?> screen, @NotNull Slot slot, GuiGraphics graphics,
                                    int x, int y, int offset) {
        var mods = getSlotModifiers(screen, slot);
        for (SlotModifier mod : mods) {
            if (mod.hasCustomColor() && mod.matches(slot)) {
                SlotModifier.renderSlotHighlight(graphics, x, y, mod.color(), mod.color2(), offset + mod.zOffset());
                return false;
            }
        }
        return true;
    }


}
