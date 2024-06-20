package net.mehvahdjukaar.polytone.slotify;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GuiModifierManager extends JsonPartialReloader {

    //slot modifiers
    private final Map<MenuType<?>, Int2ObjectArrayMap<SlotModifier>> slotsByMenuId = new IdentityHashMap<>();
    private final Map<Class<?>, Int2ObjectArrayMap<SlotModifier>> slotsByClass = new IdentityHashMap<>();
    private final Map<String, Int2ObjectArrayMap<SlotModifier>> slotsByTitle = new HashMap<>();

    //screen modifiers
    public final Map<MenuType<?>, ScreenModifier> byMenuId = new IdentityHashMap<>();
    public final Map<Class<?>, ScreenModifier> byClass = new IdentityHashMap<>();
    public final Map<String, ScreenModifier> byTitle = new HashMap<>();


    private static final ResourceLocation INVENTORY = ResourceLocation.tryParse("inventory");

    public GuiModifierManager() {
        super("gui_modifiers");
    }

    @Override
    protected void reset() {
        slotsByMenuId.clear();
        slotsByClass.clear();
        slotsByTitle.clear();
        byMenuId.clear();
        byClass.clear();
        byTitle.clear();
    }

    @Override
    protected void process(Map<ResourceLocation, JsonElement> object) {

        List<GuiModifier> allModifiers = new ArrayList<>();
        for (var entry : object.entrySet()) {
            var json = entry.getValue();
            var id = entry.getKey();
            GuiModifier modifier = GuiModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(errorMsg -> new IllegalStateException("Could not decode GUI Modifier with json id " + id + "\n error: " + errorMsg))
                    .getFirst();
            allModifiers.add(modifier);
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
                    byClass.merge(cl, new ScreenModifier(mod), (a, b) -> b.merge(a));

                    if (!mod.slotModifiers().isEmpty()) {
                        Int2ObjectArrayMap<SlotModifier> map = slotsByClass.computeIfAbsent(cl,
                                i -> new Int2ObjectArrayMap<>());
                        unwrapSlots(mod, map);
                    }

                } catch (ClassNotFoundException ignored) {
                    Polytone.LOGGER.error("Could not find class target with name {}", target);
                }


            } else if (mod.targetsMenuId()) {
                ResourceLocation menuId = ResourceLocation.tryParse(mod.target());
                boolean isInventory = menuId.equals(INVENTORY);
                Optional<MenuType<?>> menu = BuiltInRegistries.MENU.getOptional(menuId);

                if (menu.isPresent() || isInventory) {
                    byMenuId.merge(menu.orElse(null), new ScreenModifier(mod), (a, b) -> b.merge(a));

                    if (!mod.slotModifiers().isEmpty()) {
                        Int2ObjectArrayMap<SlotModifier> map = slotsByMenuId.computeIfAbsent(menu.orElse(null),
                                i -> new Int2ObjectArrayMap<>());
                        unwrapSlots(mod, map);
                    }
                }
            } else {
                //title target
                String title = mod.target();
                byTitle.merge(title, new ScreenModifier(mod), (a, b) -> b.merge(a));

                if (!mod.slotModifiers().isEmpty()) {
                    Int2ObjectArrayMap<SlotModifier> map = slotsByTitle.computeIfAbsent(title,
                            i -> new Int2ObjectArrayMap<>());
                    unwrapSlots(mod, map);
                }
            }

        }
        Polytone.LOGGER.info("Loaded GUI modifiers for: " + slotsByMenuId.keySet() + " " +
                slotsByClass.keySet() + " " + byMenuId.keySet() + " " + byClass.keySet());
    }

    private static void unwrapSlots(GuiModifier mod, Int2ObjectArrayMap<SlotModifier> map) {
        for (SlotModifier s : mod.slotModifiers()) {
            for (int i : s.targets().getSlots()) {
                //merging makes no sense we just keep last
                map.merge(i, s, SlotModifier::merge);
            }
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

    @Nullable
    public SlotModifier getSlotModifier(AbstractContainerScreen<?> screen, Slot slot) {
        Int2ObjectArrayMap<SlotModifier> m = null;
        var c = screen.getTitle();
        m = slotsByTitle.get(c.getString());
        if (m == null && c instanceof MutableComponent mc && mc.getContents() instanceof TranslatableContents tc) {
            m = slotsByTitle.get(tc.getKey());
        }
        if (m == null) {
            m = slotsByClass.get(screen.getClass());
        }
        if (m == null) slotsByClass.get(screen.getMenu().getClass());
        if (m == null) {
            MenuType<?> type;
            try {
                type = screen.getMenu().getType();
            } catch (Exception e) {
                type = null;
            }
            m = slotsByMenuId.get(type);
        }
        if (m != null) {
            return m.get(slot.index);
        }
        return null;
    }

    @Nullable
    public SlotModifier getSlotModifier(AbstractContainerMenu menu, Slot slot) {
        var m = slotsByClass.get(menu.getClass());
        if (m == null) {
            MenuType<?> type;
            try {
                type = menu.getType();
            } catch (Exception e) {
                type = null;
            }
            m = slotsByMenuId.get(type);
        }
        if (m != null) {
            return m.get(slot.index);
        }
        return null;
    }


    public void maybeModifySlot(AbstractContainerMenu menu, Slot slot) {
        var mod = getSlotModifier(menu, slot);
        if (mod != null) {
            mod.modify(slot);
        }
    }

    public boolean maybeChangeColor(AbstractContainerScreen<?> screen, Slot slot, GuiGraphics graphics,
                                           int x, int y, int offset) {
        var mod = getSlotModifier(screen, slot);
        if (mod != null && mod.hasCustomColor()) {
            mod.renderCustomHighlight(graphics, x, y, offset);
            return false;
        }
        return true;
    }


}
