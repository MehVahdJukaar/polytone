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

    // Manager -> wiki page slug. Managers without an entry (or without a file codec) just get no
    // help button. block_set, noise, custom models and colors have no dedicated page / aren't editable.
    private static Map<ContentManager<?, ?>, String> buildWikiPages() {
        Map<ContentManager<?, ?>, String> m = new IdentityHashMap<>();
        m.put(Polytone.BLOCK_MODIFIERS, "Block-Properties-Modifiers");
        m.put(Polytone.FLUID_MODIFIERS, "Fluid-Properties-Modifiers");
        m.put(Polytone.BIOME_MODIFIERS, "Biome-Effect-Modifiers");
        m.put(Polytone.BIOME_ID_MAPPERS, "Colormaps");
        m.put(Polytone.COLORMAPS, "Colormaps");
        m.put(Polytone.LIGHTMAPS, "Lightmaps");
        m.put(Polytone.CUSTOM_PARTICLES, "Custom-Particle-Types");
        m.put(Polytone.PARTICLE_MODIFIERS, "Particle-Modifiers");
        m.put(Polytone.ENTITY_MODIFIERS, "Entity-Modifiers");
        m.put(Polytone.SOUND_TYPES, "Custom-Sound-Events");
        m.put(Polytone.ITEM_MODIFIERS, "Item-Modifiers");
        m.put(Polytone.DIMENSION_MODIFIERS, "Dimension-Effects-Modifiers");
        m.put(Polytone.SLOTIFY, "Gui-Modifiers");
        m.put(Polytone.OVERLAY_MODIFIERS, "Screen-Sprite-Modifiers");
        m.put(Polytone.POST_SHADERS, "Shaders");
        m.put(Polytone.POST_TARGETS, "Shaders");
        m.put(Polytone.VARIANT_TEXTURES, "Variant-Textures");
        m.put(Polytone.CONFIGS, "Polytone-Configs");
        m.put(Polytone.CREATIVE_TABS_MODIFIERS, "Creative-Tab-Modifiers");
        m.put(Polytone.GLOBAL_EXPRESSION, "Scripting-Expressions");
        return m;
    }

    public static void init() {
        for (ContentManager<?, ?> manager : ContentManager.REGISTRY) {
            var codec = manager.contentCodec();
            if (codec == null) continue; // not editable (no file codec)
            // Only the canonical folder, never the legacy aliases (e.g. block_modifiers, not block_properties),
            // so a content type shows up once, not once per legacy parsing folder.
            String folder = manager.primaryFolder();
            if (folder == null) continue;
            String page = WIKI_PAGES.get(manager);
            String wikiUrl = page == null ? null : WIKI_BASE + page;
            NautilusStudioApi.register("Polytone", manager.name, codec, Side.CLIENT_RESOURCES,
                    Polytone.MOD_ID + "/" + folder, wikiUrl);
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
