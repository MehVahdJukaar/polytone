package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public interface SwingWidget {
    JComponent component();                          // the Swing component to add to a parent
    DataResult<JsonElement> currentJson();           // current value as JSON
    void setJson(@Nullable JsonElement value);       // populate from existing data
}
