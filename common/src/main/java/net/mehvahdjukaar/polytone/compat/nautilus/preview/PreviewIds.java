package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

// The editor hands a contentId only for read-only registry views; while editing a file it hands a path,
// so the id has to come from the pack layout.
final class PreviewIds {

    @Nullable
    static ResourceLocation of(@Nullable ResourceLocation contentId, @Nullable Path file, String folder) {
        if (contentId != null) return contentId;
        if (file == null) return null;
        int n = file.getNameCount();
        for (int i = 0; i + 3 < n; i++) {
            if (file.getName(i).toString().equals("assets")
                    && file.getName(i + 2).toString().equals(Polytone.MOD_ID)
                    && file.getName(i + 3).toString().equals(folder)) {
                String ns = file.getName(i + 1).toString();
                StringBuilder path = new StringBuilder();
                for (int j = i + 4; j < n; j++) {
                    if (!path.isEmpty()) path.append('/');
                    path.append(file.getName(j).toString());
                }
                String p = path.toString().replaceFirst("\\.json$", "");
                return p.isEmpty() ? null : ResourceLocation.fromNamespaceAndPath(ns, p);
            }
        }
        return null;
    }
}
