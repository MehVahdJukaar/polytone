package net.mehvahdjukaar.polytone.compat;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.InitializeEvent;
import io.github.fishstiz.packed_packs.api.events.InitializeLayoutEvent;
import io.github.fishstiz.packed_packs.api.events.InitializePackEntryEvent;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.gui.ChatBubbleWidget;
import net.mehvahdjukaar.polytone.content.config.ConfigsManager;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfo;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfoBadge;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfos;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public class PackedPacksCompat implements PackedPacksInitializer {

    private static @Nullable SpriteIconButton configButton;

    @Override
    public void onInitialize(PackedPacksApi api) {
        api.eventBus().register(InitializePackEntryEvent.class, Polytone.res("pack_info"), PackedPacksCompat::addBadge);
        api.eventBus().register(InitializeLayoutEvent.class, Polytone.res("config_button"), PackedPacksCompat::addConfigButton);
        api.eventBus().register(InitializeEvent.Post.class, Polytone.res("config_bubble"), PackedPacksCompat::addConfigBubble);
    }

    private static void addConfigButton(InitializeLayoutEvent event) {
        configButton = null;
        ScreenContext screen = event.screenContext();
        if (!screen.isClientResources()) return;

        if (Polytone.CONFIGS.getButtonPos() == ConfigsManager.ButtonPosition.NONE) return;

        SpriteIconButton button = Polytone.CONFIGS.makeConfigButton(20, screen.screen(), screen::reload);
        event.addWidget(InitializeLayoutEvent.Pos.AFTER_TITLE, button);
        configButton = button;
    }

    private static void addConfigBubble(InitializeEvent.Post event) {
        SpriteIconButton button = configButton;
        configButton = null;
        if (button == null) return;

        ScreenContext screen = event.screenContext();
        ChatBubbleWidget bubble = new ChatBubbleWidget(0, 0, Component.empty()).setAnimated(true);
        screen.addRenderableOnly((graphics, mouseX, mouseY, partialTick) -> {
            if (!button.visible) return;
            Component message = Polytone.CONFIGS.bubbleManager.getConfigButtonMessage(Polytone.CONFIGS.hasPackConfigs());
            if (message == null) return;

            if (!message.equals(bubble.getMessage())) {
                bubble.setText(message);
            }
            bubble.renderPointingAt(graphics, button, screen.screen().width, true, mouseX, mouseY, partialTick);
        });
    }

    private static void addBadge(InitializePackEntryEvent event) {
        ScreenContext screen = event.screenContext();
        if (!screen.isClientResources()) return;

        Pack pack = event.packContext().pack();
        String id = pack.getId();
        if (!PackInfoBadge.shouldShow(infoPage(id))) return;

        //badge hides itself while the pack sits in the available list
        Supplier<@Nullable PackInfo> info = () -> isSelected(screen, id) ? infoPage(id) : null;
        event.addTopRight(2, new PackInfoBadge(info, pack.getTitle(), screen::reload));
    }

    private static @Nullable PackInfo infoPage(String packId) {
        PackInfo info = PackInfos.get(packId);
        return info != null && info.hasInfoPage() ? info : null;
    }

    private static boolean isSelected(ScreenContext screen, String packId) {
        for (Pack p : screen.getSelectedPacks()) {
            if (p.getId().equals(packId)) return true;
        }
        return false;
    }
}
