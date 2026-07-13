package net.mehvahdjukaar.polytone.compat;

import net.mehvahdjukaar.nautilus.NautilusStudioApi;
import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ContentManager;

import java.util.IdentityHashMap;
import java.util.Map;

public final class PolytoneNautilus {

    private static final String WIKI_BASE = "https://github.com/MehVahdJukaar/polytone/wiki/";

    // Manager -> wiki page slug, so the editor tab shows a "help" button linking to its docs.
    private static final Map<ContentManager<?, ?>, String> WIKI_PAGES = buildWikiPages();

    // Only the ContentManager-backed managers are registered with the editor (see init loop below),
    // so only those need a wiki page. block_set has no dedicated page and is left out.
    private static Map<ContentManager<?, ?>, String> buildWikiPages() {
        Map<ContentManager<?, ?>, String> m = new IdentityHashMap<>();
        m.put(Polytone.BLOCK_MODIFIERS, "Block-Properties-Modifiers");
        m.put(Polytone.FLUID_MODIFIERS, "Fluid-Properties-Modifiers");
        m.put(Polytone.BIOME_MODIFIERS, "Biome-Effect-Modifiers");
        m.put(Polytone.BIOME_ID_MAPPERS, "Colormaps");
        m.put(Polytone.COLORMAPS, "Colormaps");
        m.put(Polytone.CUSTOM_PARTICLES, "Custom-Particle-Types");
        m.put(Polytone.PARTICLE_MODIFIERS, "Particle-Modifiers");
        m.put(Polytone.ENTITY_MODIFIERS, "Entity-Modifiers");
        m.put(Polytone.SOUND_TYPES, "Custom-Sound-Events");
        return m;
    }

    public static void init() {
        for (ContentManager<?, ?> manager : ContentManager.REGISTRY) {
            var codec = manager.contentCodec();
            if (codec == null) continue; // not editable (no file codec)
            String page = WIKI_PAGES.get(manager);
            String wikiUrl = page == null ? null : WIKI_BASE + page;
            for (String folder : manager.folderNames()) {
                NautilusStudioApi.register("Polytone", manager.name, codec, Side.CLIENT_RESOURCES,
                        Polytone.MOD_ID + "/" + folder, wikiUrl);
            }
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
