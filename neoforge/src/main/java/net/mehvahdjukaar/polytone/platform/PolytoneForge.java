package net.mehvahdjukaar.polytone.platform;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.content.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.content.slotify.GuiModifierOverlay;
import net.mehvahdjukaar.polytone.content.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.content.tabs.ItemPredicate;
import net.mehvahdjukaar.polytone.content.tabs.ItemToTabEvent;
import net.mehvahdjukaar.polytone.mixins.neoforge.BuildCreativeModeTabContentsEventAccessor;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Author: MehVahdJukaar
 */
@Mod("polytone")
public class PolytoneForge {
    public static final Logger LOGGER = LogManager.getLogger("Polytone");

    static IEventBus bus;

    public PolytoneForge(IEventBus modBus) {
        bus = modBus;
        if (FMLEnvironment.dist == Dist.CLIENT) {
            boolean iris = ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");
            Polytone.init(!FMLEnvironment.production, true, iris);
            NeoForge.EVENT_BUS.register(this);
            modBus.addListener(EventPriority.LOWEST, this::modifyCreativeTabs);
        } else {
            LOGGER.warn("Polytone has been installed on a server. This wont cause issues but mod wont do anything here as its a client mod");
        }

        ModList.get().getModContainerById(Polytone.MOD_ID).ifPresent(container ->
                container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, modListScreen) ->
                        Polytone.CONFIGS.createScreenForMainMenu(modListScreen)));
    }

    //@SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        Polytone.ENTITY_MODIFIERS.onEntityTick(event.getEntity());
    }

    @SubscribeEvent
    public void onTick(LevelTickEvent.Pre event) {
        // LevelTickEvent fires for EVERY level, including the integrated server's level on the server
        // thread. ClientFrameTicker is client-only and drives GL work (post shader chain loading), so
        // only forward the client level tick - matches Fabric, which ticks client.level exclusively.
        if (!event.getLevel().isClientSide()) return;
        ClientFrameTicker.onTick(event.getLevel());
    }

    @SubscribeEvent
    public void onRender(RenderFrameEvent.Pre onRender) {
        ClientFrameTicker.onRenderTick(Minecraft.getInstance());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltip(ItemTooltipEvent tooltipEvent) {
        var mod = ((IPolytoneItem) tooltipEvent.getItemStack().getItem()).polytone$getModifier();
        if (mod != null) {
            mod.modifyTooltips(tooltipEvent.getToolTip());
        }
    }

    @SubscribeEvent
    public void onTagSync(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            Polytone.onTagsReceived(event.getRegistryAccess());
        }
        bus = null;
    }

    @SubscribeEvent
    public void renderStageEventAfterLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL ) {
            PolytoneRenderTypes.onRenderLast();
        }
    }

    @SubscribeEvent
    public void renderScreen(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof SlotifyScreen ss)) return;
        GuiModifierOverlay.renderScreenExtras(event.getGuiGraphics(), ss, screen.width, screen.height,
                event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }


    @SubscribeEvent
    public void fogEvent(ViewportEvent.RenderFog fogEvent) {
        if (fogEvent.getType() != FogType.NONE || fogEvent.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        Vec2 targetFog = Polytone.BIOME_MODIFIERS.modifyFogParameters(fogEvent.getNearPlaneDistance(), fogEvent.getFarPlaneDistance());
        if (targetFog != null) {
            fogEvent.setNearPlaneDistance(targetFog.x);
            fogEvent.setFarPlaneDistance(targetFog.y);
            fogEvent.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Polytone.onLoggedOut();
    }

    public void modifyCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        Polytone.CREATIVE_TABS_MODIFIERS.modifyTab(new ItemToTabEventImpl(event));
    }

    public record ItemToTabEventImpl(BuildCreativeModeTabContentsEvent event) implements ItemToTabEvent {

        @Override
        public ResourceKey<CreativeModeTab> getTab() {
            return event.getTabKey();
        }

        @Override
        public Collection<ItemStack> getAllItems() {
            return event.getParentEntries();
        }

        @Override
        public void removeItems(Predicate<ItemStack> target) {
            BuildCreativeModeTabContentsEventAccessor acc = ((BuildCreativeModeTabContentsEventAccessor) (Object) event);
            acc.getParentEntries().removeIf(target);
            acc.getSearchEntries().removeIf(target);
        }

        @Override
        public void addItems(@Nullable Predicate<ItemStack> target, boolean after, List<ItemStack> items) {
            if (target == null || target == ItemPredicate.TRUE_PRED || !event.getTab().hasAnyItems()) {
                event.acceptAll(items);
            } else {
                if (after) {
                    ItemStack last = findLast(event, target);
                    if (last.isEmpty()) {
                        return;
                    }
                    for (int j = items.size(); j > 0; j--) {
                        event.insertAfter(last, items.get(j - 1), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    }
                } else {
                    ItemStack first = findFirst(event, target);
                    if (first.isEmpty()) {
                        return;
                    }
                    for (var s : items) {
                        event.insertBefore(first, s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    }
                }
            }
        }

        private ItemStack findFirst(BuildCreativeModeTabContentsEvent event, Predicate<ItemStack> target) {
            for (var s : event.getParentEntries()) {
                if (target.test(s)) {
                    return s;
                }
            }
            Polytone.LOGGER.error("Could not find target item in creative tab {}", event.getTab());
            return ItemStack.EMPTY;
        }

        private ItemStack findLast(BuildCreativeModeTabContentsEvent event, Predicate<ItemStack> target) {
            boolean foundOne = false;
            ItemStack previous = ItemStack.EMPTY;
            for (var s : event.getParentEntries()) {
                if (target.test(s)) {
                    foundOne = true;
                    previous = s;
                } else {
                    if (foundOne) return previous;
                }
            }
            return previous;
        }
    }

}
