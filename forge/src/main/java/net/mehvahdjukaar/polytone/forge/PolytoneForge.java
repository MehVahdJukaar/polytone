package net.mehvahdjukaar.polytone.forge;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.tabs.ItemToTabEvent;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Author: MehVahdJukaar
 */
@Mod("polytone")
public class PolytoneForge {
    public static final Logger LOGGER = LogManager.getLogger("Polytone");

    public PolytoneForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Polytone.init(false, !FMLEnvironment.production, true);

            MinecraftForge.EVENT_BUS.register(this);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(EventPriority.LOWEST, this::modifyCreativeTabs);
        } else {
            LOGGER.warn("Slotify has been installed on a server. This wont cause issues but mod wont do anything here as its a client mod");
        }
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent onRender) {
        if (onRender.phase == TickEvent.Phase.START)
            ClientFrameTicker.onRenderTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public void onTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ClientFrameTicker.onTick(event.level);
        }
    }

    @SubscribeEvent
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
    }

    @SubscribeEvent
    public void renderScreen(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        SlotifyScreen ss = (SlotifyScreen) screen;
        if (ss.polytone$hasSprites()) {

            GuiGraphics graphics = event.getGuiGraphics();
            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            poseStack.translate(screen.width / 2F, screen.height / 2F, 500);
            ss.polytone$renderExtraSprites(graphics);
            poseStack.popPose();
        }
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
    public void onLevelUnload(ClientPlayerNetworkEvent.LoggingOut event) {
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
        public void removeItems(Predicate<ItemStack> target) {
            var iter = event.getEntries().iterator();
            while (iter.hasNext()) {
                var e = iter.next();
                if (target.test(e.getKey())) {
                    iter.remove();
                }
            }
        }

        @Override
        public void addItems(@Nullable Predicate<ItemStack> target, boolean after, List<ItemStack> items) {

            if (target == null) {
                event.acceptAll(items);
            } else {

                var entries = event.getEntries();
                ItemStack lastValid = null;


                for (var e : entries) {
                    ItemStack item = e.getKey();

                    if (!item.isItemEnabled(event.getFlags())) continue;

                    boolean isValid = target.test(item);
                    if (after && lastValid != null && !isValid) {
                        var rev = Lists.reverse(new ArrayList<>(items));
                        for (var ni : rev) {
                            entries.putAfter(lastValid, ni, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                        }
                        return;
                    }

                    if (isValid) {
                        lastValid = item;
                    }

                    if (!after && isValid) {
                        items.forEach(ni -> entries.putBefore(item, ni, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
                        return;
                    }
                }
                //add at the end if it fails
                for (var ni : items) {
                    entries.put(ni, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                }
            }
        }
    }
}
