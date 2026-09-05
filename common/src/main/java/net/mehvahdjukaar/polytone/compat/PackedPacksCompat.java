package net.mehvahdjukaar.polytone.compat;

import io.github.fishstiz.packed_packs.api.PackedPacksApi;
import io.github.fishstiz.packed_packs.api.PackedPacksInitializer;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.InitializePackEntryEvent;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfo;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfoBadge;
import net.mehvahdjukaar.polytone.content.packinfo.PackInfos;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public class PackedPacksCompat implements PackedPacksInitializer {

    @Override
    public void onInitialize(PackedPacksApi api) {
        api.eventBus().register(InitializePackEntryEvent.class, Polytone.res("pack_info"), PackedPacksCompat::addBadge);
    }

    private static void addBadge(InitializePackEntryEvent event) {
        ScreenContext screen = event.screenContext();
        if (!screen.isClientResources()) return;

        Pack pack = event.packContext().pack();
        String id = pack.getId();
        if (!PackInfoBadge.shouldShow(PackInfos.get(id))) return;

        Supplier<@Nullable PackInfo> info = () -> isSelected(screen, id) ? PackInfos.get(id) : null;
        event.addTopRight(2, new PackInfoBadge(info, pack.getTitle(), screen::reload));
    }

    private static boolean isSelected(ScreenContext screen, String packId) {
        for (Pack p : screen.getSelectedPacks()) {
            if (p.getId().equals(packId)) return true;
        }
        return false;
    }
}
