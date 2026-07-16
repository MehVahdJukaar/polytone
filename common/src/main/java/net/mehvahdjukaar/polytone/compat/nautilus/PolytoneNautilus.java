package net.mehvahdjukaar.polytone.compat.nautilus;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.nautilus.NautilusStudioApi;
import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.nautilus.workbench.CodecEntry;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ContentManager;

public final class PolytoneNautilus {

    private static final String WIKI_BASE = "https://github.com/MehVahdJukaar/polytone/wiki/";

    public static void init() {
        for (ContentManager<?, ?> manager : ContentManager.REGISTRY) {
            Codec<?> codec = manager.contentCodec();
            if (codec == null) continue; // not editable (no file codec)
            // Only the canonical folder, never the legacy aliases (e.g. block_modifiers, not block_properties),
            // so a content type shows up once, not once per legacy parsing folder.
            String folder = manager.primaryFolder();
            if (folder == null) continue;

            // Nautilus derives the schema from the raw codec itself - Polytone doesn't need to build it.
            CodecEntry entry = new CodecEntry(manager.name, "Polytone", SchemaCodec.wrap(codec), Side.CLIENT_RESOURCES,
                    Polytone.MOD_ID + "/" + folder);

            // Content types with companion textures (colormaps, block/fluid/particle tints) show their
            // sibling .png files in the editor via the SAME naming contract the reload driver uses.
            if (manager.companions != null) {
                entry = entry.withSidecars(CompanionSidecars.of(manager.companions, Side.CLIENT_RESOURCES));
            }

            String page = manager.wikiPage();
            if (page != null) entry = entry.withWikiUrl(WIKI_BASE + page);

            NautilusStudioApi.register(entry);
        }
    }

    public static void open() {
        NautilusStudioApi.openEditor();
    }

    public static boolean isOpen() {
        return NautilusStudioApi.isOpen();
    }

    public static void close() {
        NautilusStudioApi.close();
    }

}
