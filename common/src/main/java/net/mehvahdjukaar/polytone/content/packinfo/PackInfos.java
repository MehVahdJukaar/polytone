package net.mehvahdjukaar.polytone.content.packinfo;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.server.packs.PackResources;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PackInfos {

    private static final Map<String, PackInfo> BY_PACK_ID = new ConcurrentHashMap<>();

    public static void readFrom(PackResources packResources) {
        String id = packResources.packId();
        try {
            PackInfo info = packResources.getMetadataSection(PackInfo.TYPE);
            if (info != null && !info.isEmpty()) {
                BY_PACK_ID.put(id, info);
            } else {
                BY_PACK_ID.remove(id);
            }
        } catch (Exception e) {
            BY_PACK_ID.remove(id);
            Polytone.LOGGER.warn("Failed to read Polytone info section of pack {}", id, e);
        }
    }

    public static @Nullable PackInfo get(String packId) {
        return BY_PACK_ID.get(packId);
    }
}
