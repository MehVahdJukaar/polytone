package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import net.mehvahdjukaar.polytone.common.codec_ui.Schema;

/**
 * Named, reusable factory for a custom widget bound to a codec via
 * {@link net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodecs#withWidget}.
 *
 * <p>Idiomatic usage: declare a {@code public static final SwingWidgetDef<X> DEF = ...}
 * on the widget class itself, then reference {@code MyWidget.DEF} in codec declarations.
 * No global registry, no Identifier strings. This mirrors how vanilla Minecraft separates
 * {@code BlockEntityType<>} / {@code RuleTestType<>} defs from their instances.</p>
 *
 * @param <A> the data type the widget edits
 */
@FunctionalInterface
public interface SwingWidgetDef<A> {
    SwingWidget create(Schema.Custom<A> schema);
}
