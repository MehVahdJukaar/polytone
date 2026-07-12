package net.mehvahdjukaar.polytone.compat;

import net.mehvahdjukaar.nautilus.NautilusStudioApi;
import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ContentManager;

/**
 * Polytone's integration with the standalone <b>Nautilus Studio</b> pack editor mod: the one class
 * that touches its API. It registers every codec-backed {@link ContentManager} as an editable
 * content type, then delegates open/close to the editor.
 *
 * <p>Everything here references {@code nautilus_studio} classes, so callers MUST guard on that mod
 * being loaded ({@code PlatStuff.isModLoaded("nautilus_studio")}) — with the mod absent this class
 * is never referenced, its classes never load, and the in-game editor button is simply not shown.</p>
 *
 * <p>1.21.1 port of the 1.21.11 {@code compat.PackEditor}: kept minimal — it registers content
 * codecs (rich schema where a manager provides a hand-built {@code SchemaCodec}, raw-JSON fallback
 * otherwise). The 1.21.11 widget bindings + companion sidecars are follow-up work.</p>
 */
public final class PolytoneEditor {

    /** Register Polytone's editable content with the editor. Call once when nautilus_studio is present. */
    public static void init() {
        for (ContentManager<?, ?> manager : ContentManager.REGISTRY) {
            var codec = manager.contentCodec();
            if (codec == null) continue; // not editable (no file codec)
            for (String folder : manager.folderNames()) {
                NautilusStudioApi.register("Polytone", manager.name, codec, Side.CLIENT_RESOURCES,
                        Polytone.MOD_ID + "/" + folder);
            }
        }
    }

    /** Open (or focus) the editor window. Any thread. */
    public static void open() {
        NautilusStudioApi.openEditor();
    }

    /** Whether the editor window is currently open. Any thread. */
    public static boolean isOpen() {
        return NautilusStudioApi.isOpen();
    }

    /** Close the editor window if open. Any thread. */
    public static void close() {
        NautilusStudioApi.close();
    }

    private PolytoneEditor() {}
}
