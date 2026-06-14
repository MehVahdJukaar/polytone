package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import net.mehvahdjukaar.polytone.common.codec_ui.Schema;

public final class SwingWidgetFactory {

    private SwingWidgetFactory() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static SwingWidget create(Schema<?> schema) {
        // Custom: dispatch via a SwingWidgetDef stored opaquely in Schema.Custom.
        // Any Schema.Custom whose widgetDef isn't a SwingWidgetDef (e.g. a def for a
        // different UI backend) falls through to the OpaqueWidget JSON fallback below.
        if (schema instanceof Schema.Custom<?> c
                && c.widgetDef() instanceof SwingWidgetDef<?> def) {
            return ((SwingWidgetDef) def).create((Schema.Custom) c);
        }
        if (schema instanceof Schema.Bool) {
            return new BoolWidget();
        }
        if (schema instanceof Schema.Color c) {
            return new ColorWidget(c);
        }
        if (schema instanceof Schema.IntRange r) {
            int initial = clampInt(0, r.min(), r.max());
            return new NumberWidget(NumberWidget.Kind.INT, r.min(), r.max(), 1, initial);
        }
        if (schema instanceof Schema.LongRange r) {
            long initial = clampLong(0L, r.min(), r.max());
            return new NumberWidget(NumberWidget.Kind.LONG, r.min(), r.max(), 1L, initial);
        }
        if (schema instanceof Schema.FloatRange r) {
            float initial = clampFloat(0f, r.min(), r.max());
            return new NumberWidget(NumberWidget.Kind.FLOAT, r.min(), r.max(), 0.1f, initial);
        }
        if (schema instanceof Schema.DoubleRange r) {
            double initial = clampDouble(0d, r.min(), r.max());
            return new NumberWidget(NumberWidget.Kind.DOUBLE, r.min(), r.max(), 0.1d, initial);
        }
        if (schema instanceof Schema.Str s) {
            return new StringWidget(s.maxLen());
        }
        if (schema instanceof Schema.ResourceId rid) {
            return new ResourceIdWidget(rid);
        }
        if (schema instanceof Schema.Record<?> rec) {
            return new RecordWidget(rec);
        }
        if (schema instanceof Schema.ListOf<?> list) {
            return new ListWidget(list);
        }
        if (schema instanceof Schema.Enum<?> e) {
            return new EnumWidget(e);
        }
        if (schema instanceof Schema.OneOf<?> oneOf) {
            return new OneOfWidget(oneOf);
        }
        if (schema instanceof Schema.MapOf<?, ?> map) {
            return new MapOfWidget(map);
        }
        if (schema instanceof Schema.PairOf<?, ?> pair) {
            return new PairOfWidget(pair);
        }
        if (schema instanceof Schema.EitherOf<?, ?> either) {
            return new EitherOfWidget(either);
        }
        // Opaque, or Custom with a non-Swing widgetDef — opaque JSON fallback.
        return new OpaqueWidget();
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
    private static long clampLong(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }
    private static float clampFloat(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
    private static double clampDouble(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
