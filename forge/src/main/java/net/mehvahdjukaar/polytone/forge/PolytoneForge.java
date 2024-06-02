package net.mehvahdjukaar.polytone.forge;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.slotify.SlotifyScreen;
import net.mehvahdjukaar.polytone.tabs.ItemToTabEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraftforge.event.level.LevelEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Author: MehVahdJukaar
 */
@Mod(Polytone.MOD_ID)
public class PolytoneForge {

    public PolytoneForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Polytone.init(false, !FMLEnvironment.production);

            NeoForge.EVENT_BUS.register(this);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::modifyCreativeTabs);
        } else {
            Polytone.LOGGER.warn("Polytone has been installed on a server. This wont cause issues but mod wont do anything here as its a client mod");
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

            PoseStack poseStack = event.getGuiGraphics().pose();
            poseStack.pushPose();
            poseStack.translate(screen.width / 2F, screen.height / 2F, 500);
            ss.polytone$renderExtraSprites(poseStack);
            poseStack.popPose();
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
