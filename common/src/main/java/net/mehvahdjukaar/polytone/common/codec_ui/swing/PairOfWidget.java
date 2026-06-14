package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

public final class PairOfWidget implements SwingWidget {

    private final SwingWidget firstWidget;
    private final SwingWidget secondWidget;
    private final JPanel root = new JPanel();

    public PairOfWidget(Schema.PairOf<?, ?> schema) {
        this.firstWidget = SwingWidgetFactory.create(schema.first());
        this.secondWidget = SwingWidgetFactory.create(schema.second());

        root.setLayout(new GridLayout(1, 2, UiScale.med(), 0));

        JPanel firstCol = new JPanel(new BorderLayout(0, UiScale.small()));
        firstCol.add(new JLabel("first"), BorderLayout.NORTH);
        firstCol.add(firstWidget.component(), BorderLayout.CENTER);

        JPanel secondCol = new JPanel(new BorderLayout(0, UiScale.small()));
        secondCol.add(new JLabel("second"), BorderLayout.NORTH);
        secondCol.add(secondWidget.component(), BorderLayout.CENTER);

        root.add(firstCol);
        root.add(secondCol);

        // Stretch in parent so each column can use its half of the available width.
        root.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public DataResult<JsonElement> currentJson() {
        // TODO: DFU's PairCodec encodes as an object with first/second keys (or a merged map),
        // not necessarily a 2-element array. This MVP shape may not round-trip cleanly.
        DataResult<JsonElement> firstResult = firstWidget.currentJson();
        var firstError = firstResult.error();
        if (firstError.isPresent()) {
            String msg = firstError.get().message();
            return DataResult.error(() -> "Pair.first: " + msg);
        }
        DataResult<JsonElement> secondResult = secondWidget.currentJson();
        var secondError = secondResult.error();
        if (secondError.isPresent()) {
            String msg = secondError.get().message();
            return DataResult.error(() -> "Pair.second: " + msg);
        }
        JsonArray array = new JsonArray();
        firstResult.result().ifPresent(array::add);
        secondResult.result().ifPresent(array::add);
        return DataResult.success(array);
    }

    @Override
    public void setJson(@Nullable JsonElement value) {
        if (value != null && value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            if (array.size() > 0) firstWidget.setJson(array.get(0));
            if (array.size() > 1) secondWidget.setJson(array.get(1));
        }
    }
}
