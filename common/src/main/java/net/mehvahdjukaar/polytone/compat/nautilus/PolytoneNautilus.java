package net.mehvahdjukaar.polytone.compat.nautilus;

import net.mehvahdjukaar.nautilus.NautilusStudioApi;
import net.mehvahdjukaar.nautilus.compat.polytone.PolytoneCompat;

//other side of compat in nautilus. Not here not to clutter files and avoid extra very optional dep.
public final class PolytoneNautilus {

    public static void init() {
        PolytoneCompat.init();
    }

    public static void open() {
        NautilusStudioApi.openEditor();
    }

    public static boolean isOpen() {
        return NautilusStudioApi.isOpen();
    }
}
