package net.mehvahdjukaar.polytone.common.codec_ui;

import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public interface SchemaEditor {
    /**
     * Opens an editor for the given codec. On save, the editor:
     *  (a) writes the JSON encoding to a file (implementation chooses path UX),
     *  (b) calls {@code onSave} with the parsed value.
     * The editor must not block the calling thread.
     */
    <A> void open(SchemaCodec<A> codec, @Nullable A initial, Consumer<A> onSave);
}
