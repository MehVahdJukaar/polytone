package net.mehvahdjukaar.polytone.slotify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GuiModifierManager extends SimpleJsonResourceReloadListener {

    private static final Map<MenuType<?>, Int2ObjectArrayMap<SlotModifier>> SLOTS_BY_MENU_ID = new IdentityHashMap<>();
    private static final Map<Class<?>, Int2ObjectArrayMap<SlotModifier>> SLOTS_BY_CLASS = new IdentityHashMap<>();
    private static final Map<String, Int2ObjectArrayMap<SlotModifier>> SLOTS_BY_TITLE = new HashMap<>();
    public static final Map<MenuType<?>, ScreenModifier> BY_MENU_ID = new IdentityHashMap<>();
    public static final Map<Class<?>, ScreenModifier> BY_CLASS = new IdentityHashMap<>();
    public static final Map<String, ScreenModifier> BY_TITLE = new HashMap<>();


    private static final ResourceLocation INVENTORY = new ResourceLocation("inventory");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String GUI_MODIFIERS = "gui_modifiers";

    public GuiModifierManager() {
        super(GSON, Polytone.MOD_ID + "/" + GUI_MODIFIERS);
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> map = super.prepare(resourceManager, profiler);
        //backwards compat with slotify
        scanDirectory(resourceManager, GUI_MODIFIERS, GSON, map);
        return map;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        SLOTS_BY_MENU_ID.clear();
        SLOTS_BY_CLASS.clear();
        SLOTS_BY_TITLE.clear();
        BY_MENU_ID.clear();
        BY_CLASS.clear();
        BY_TITLE.clear();

        List<GuiModifier> allModifiers = new ArrayList<>();
        for (var entry : object.entrySet()) {
            var json = entry.getValue();
            var id = entry.getKey();
            GuiModifier modifier = GuiModifier.CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode GUI Modifier with json res {} - error: {}",
                            id, errorMsg)).getFirst();
            allModifiers.add(modifier);
        }

        for (GuiModifier mod : allModifiers) {
            //inventory has a null menu type for some reason
            if (mod.targetsClass()) {
                try {
                    var cl = Class.forName(mod.target());
                    BY_CLASS.merge(cl, new ScreenModifier(mod), (a, b) -> b.merge(a));

                    if (!mod.slotModifiers().isEmpty()) {
                        Int2ObjectArrayMap<SlotModifier> map = SLOTS_BY_CLASS.computeIfAbsent(cl,
                                i -> new Int2ObjectArrayMap<>());
                        unwrapSlots(mod, map);
                    }

                } catch (ClassNotFoundException ignored) {
                }


            } else if (mod.targetsMenuId()) {
                ResourceLocation menuId = new ResourceLocation(mod.target());
                boolean isInventory = menuId.equals(INVENTORY);
                Optional<MenuType<?>> menu = BuiltInRegistries.MENU.getOptional(menuId);

                if (menu.isPresent() || isInventory) {
                    BY_MENU_ID.merge(menu.orElse(null), new ScreenModifier(mod), (a, b) -> b.merge(a));

                    if (!mod.slotModifiers().isEmpty()) {
                        Int2ObjectArrayMap<SlotModifier> map = SLOTS_BY_MENU_ID.computeIfAbsent(menu.orElse(null),
                                i -> new Int2ObjectArrayMap<>());
                        unwrapSlots(mod, map);
                    }
                }
            } else {
                //title target
                String title = mod.target();
                BY_TITLE.merge(title, new ScreenModifier(mod), (a, b) -> b.merge(a));

                if (!mod.slotModifiers().isEmpty()) {
                    Int2ObjectArrayMap<SlotModifier> map = SLOTS_BY_TITLE.computeIfAbsent(title,
                            i -> new Int2ObjectArrayMap<>());
                    unwrapSlots(mod, map);
                }
            }

        }
        Polytone.LOGGER.info("Loaded modifiers for: " + SLOTS_BY_MENU_ID.keySet() + " " +
                SLOTS_BY_CLASS.keySet() + " " + BY_MENU_ID.keySet() + " " + BY_CLASS.keySet());
    }

    private static void unwrapSlots(GuiModifier mod, Int2ObjectArrayMap<SlotModifier> map) {
        for (SlotModifier s : mod.slotModifiers()) {
            for (int i : s.targets().getSlots()) {
                //merging makes no sense we just keep last
                map.merge(i, s, SlotModifier::merge);
            }
        }
    }

    private static ScreenModifier getScreenModifier(AbstractContainerScreen<?> screen) {
        ScreenModifier m = null;
        AbstractContainerMenu menu = screen.getMenu();
        if (menu != null) {
            m = BY_CLASS.get(menu.getClass());
        }
        if (m == null) {
            MenuType<?> type;
            try {
                type = menu.getType();
            } catch (Exception e) {
                type = null;
            }
            m = BY_MENU_ID.get(type);
        }
        return m;
    }

    @Nullable
    public static ScreenModifier getGuiModifier(Screen screen) {
        ScreenModifier m = null;
        var c = screen.getTitle();
        m = BY_TITLE.get(c.getString());
        if (m == null && c instanceof MutableComponent mc && mc.getContents() instanceof TranslatableContents tc) {
            m = BY_TITLE.get(tc.getKey());
        }
        if (m == null) {
            m = BY_CLASS.get(screen.getClass());
        }
        if (m == null && screen instanceof AbstractContainerScreen<?> as) {
            m = getScreenModifier(as);
        }

        return m;
    }

    @Nullable
    public static SlotModifier getSlotModifier(AbstractContainerScreen<?> screen, Slot slot) {
        Int2ObjectArrayMap<SlotModifier> m = null;
        var c = screen.getTitle();
        m = SLOTS_BY_TITLE.get(c.getString());
        if (m == null && c instanceof MutableComponent mc && mc.getContents() instanceof TranslatableContents tc) {
            m = SLOTS_BY_TITLE.get(tc.getKey());
        }
        if (m == null) {
            m = SLOTS_BY_CLASS.get(screen.getClass());
        }
        if (m == null) SLOTS_BY_CLASS.get(screen.getMenu().getClass());
        if (m == null) {
            MenuType<?> type;
            try {
                type = screen.getMenu().getType();
            } catch (Exception e) {
                type = null;
            }
            m = SLOTS_BY_MENU_ID.get(type);
        }
        if (m != null) {
            return m.get(slot.index);
        }
        return null;
    }

    @Nullable
    public static SlotModifier getSlotModifier(AbstractContainerMenu menu, Slot slot) {
        var m = SLOTS_BY_CLASS.get(menu.getClass());
        if (m == null) {
            MenuType<?> type;
            try {
                type = menu.getType();
            } catch (Exception e) {
                type = null;
            }
            m = SLOTS_BY_MENU_ID.get(type);
        }
        if (m != null) {
            return m.get(slot.index);
        }
        return null;
    }


    public static void maybeModifySlot(AbstractContainerMenu menu, Slot slot) {
        var mod = getSlotModifier(menu, slot);
        if (mod != null) {
            mod.modify(slot);
        }
    }

    public static boolean maybeChangeColor(AbstractContainerScreen<?> screen, Slot slot, GuiGraphics graphics,
                                           int x, int y, int offset) {
        var mod = getSlotModifier(screen, slot);
        if (mod != null && mod.hasCustomColor()) {
            mod.renderCustomHighlight(graphics, x, y, offset);
            return false;
        }
        return true;
    }
}
