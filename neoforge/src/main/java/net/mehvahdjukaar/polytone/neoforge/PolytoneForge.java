package net.mehvahdjukaar.polytone.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.item.IPolytoneItem;
import net.mehvahdjukaar.polytone.mixins.neoforge.BuildCreativeModeTabContentsEventAccessor;
import net.mehvahdjukaar.polytone.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.tabs.ItemToTabEvent;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.minecraft.client.resources.model.ModelBakery.ITEM_MODEL_GENERATOR;

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
            modBus.addListener(this::addAdditionalModels);
            modBus.addListener(this::test);
        } else {
            LOGGER.warn("Polytone has been installed on a server. This wont cause issues but mod wont do anything here as its a client mod");
        }

    }

    public void test(ModelEvent.ModifyBakingResult modifyBakingResult){
        int aa = 1;
    }

    public void addAdditionalModels(ModelEvent.RegisterAdditional event) {
        Polytone.ITEM_MODELS.getExtraModels().forEach(e -> {
            event.register(ModelResourceLocation.standalone(e));
        });
    }

    @SubscribeEvent
    public void onTick(LevelTickEvent.Pre event) {
        ClientFrameTicker.onTick(event.getLevel());
    }

    @SubscribeEvent
    public void onRender(RenderFrameEvent.Pre onRender) {
        ClientFrameTicker.onRenderTick(Minecraft.getInstance());
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
            Polytone.onTagsReceived(event.getLookupProvider());
        }
        bus = null;
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
            ss.polytone$renderExtraSprites(graphics, event.getMouseX(),event.getMouseY(), event.getPartialTick());
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
    public void onLevelUnload(LevelEvent.Unload event) {
        Polytone.onLevelUnload();
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
            if (target == null) {
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
