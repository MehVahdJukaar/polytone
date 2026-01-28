package net.mehvahdjukaar.polytone.neoforge;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.content.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.content.particle.debug.ParticleHitboxDebugRenderer;
import net.mehvahdjukaar.polytone.content.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.content.tabs.ItemPredicate;
import net.mehvahdjukaar.polytone.content.tabs.ItemToTabEvent;
import net.mehvahdjukaar.polytone.mixins.neoforge.BuildCreativeModeTabContentsEventAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugEntryNoop;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

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
        SpecialModelsHandlerImpl.init(bus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            Polytone.init(!FMLEnvironment.isProduction(), true);
            NeoForge.EVENT_BUS.register(this);
            modBus.addListener(EventPriority.LOWEST, this::modifyCreativeTabs);
            modBus.addListener(this::onRegisterDebugEntries);
        } else {
            LOGGER.warn("Polytone has been installed on a server. This wont cause issues but mod wont do anything here as its a client mod");
        }

    }

    public void onRegisterDebugEntries(RegisterDebugEntriesEvent event) {
        event.register(ParticleHitboxDebugRenderer.ID, new DebugEntryNoop());
        event.includeInProfile(ParticleHitboxDebugRenderer.ID, DebugScreenProfile.DEFAULT,
                DebugScreenEntryStatus.ALWAYS_ON);
        event.includeInProfile(ParticleHitboxDebugRenderer.ID, DebugScreenProfile.PERFORMANCE,
                DebugScreenEntryStatus.ALWAYS_ON);
    }

    @SubscribeEvent
    public void renderVistaDebug(RenderLevelStageEvent.AfterTripwireBlocks event) {
        ParticleHitboxDebugRenderer.emitGizmos();
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        Polytone.ENTITY_MODIFIERS.onEntityTick(event.getEntity());
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) Polytone.onTick(level);
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

    /*
    @SubscribeEvent
    public <A extends LivingEntityRenderState> void onRenderEntityPost(RenderLivingEvent.Post<?, A, ?> event) {
        Polytone.ENTITY_MODIFIERS.onEntityRender(event.getRenderer(),
                event.getPoseStack(), event.getRenderState(), event.getCameraState());

    }*/

    //tag sync event seems to be broken in latest neo
    @SubscribeEvent
    public void onTagSync(ClientPlayerNetworkEvent.LoggingIn event) {
        Polytone.onTagsReceived(event.getPlayer().registryAccess());
        bus = null;
    }

    @SubscribeEvent
    public void renderStageEventAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        PolytoneRenderTypes.onRenderLast();
    }

    @SubscribeEvent
    public void renderStageEventAfterLevel(RenderLevelStageEvent.AfterParticles event) {
        PolytoneRenderTypes.cacheMatrices();
    }


    @SubscribeEvent
    public void renderScreen(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        SlotifyScreen ss = (SlotifyScreen) screen;
        if (ss.polytone$hasSprites()) {

            GuiGraphics graphics = event.getGuiGraphics();
            graphics.nextStratum();
            Matrix3x2fStack poseStack = graphics.pose();
            poseStack.pushMatrix();
            poseStack.translate(screen.width / 2F, screen.height / 2F);
            ss.polytone$renderExtraSprites(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
            poseStack.popMatrix();
        }
    }

    @SubscribeEvent
    public void onLevelUnload(ClientPlayerNetworkEvent.LoggingOut event) {
        Polytone.onLogOut();
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
