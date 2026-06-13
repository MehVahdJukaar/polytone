package net.mehvahdjukaar.polytone.common.codec_ui.swing;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.Enumeration;

/**
 * Runtime DPI helper for our Swing editor.
 *
 * <p>NeoForge sets {@code java.awt.headless=true} at JVM launch which forces
 * {@code GraphicsEnvironment} to initialise before any of our code runs, so
 * setting {@code sun.java2d.uiScale=auto} after the fact is a no-op. Instead we
 * detect screen DPI on first access and apply an explicit scale factor to every
 * Swing default and to manually-sized components.
 *
 * <p>Caveat: {@link Toolkit#getScreenResolution()} returns the actual DPI on
 * hi-DPI Linux/Windows (typically 192 on 4K). macOS Swing handles retina scaling
 * at the OS level and reports 72/96, so this scaler becomes a no-op there — which
 * is what we want. Multi-monitor setups with mixed DPI use a single global factor.
 */
public final class UiScale {
    private static final float SCALE; // 1.0 on 96dpi displays, 2.0 on 192dpi (typical 4K)
    private static volatile boolean applied = false;

    static {
        int dpi;
        try {
            dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        } catch (Throwable t) {
            dpi = 96;
        }
        SCALE = Math.max(1.0f, dpi / 96.0f);
    }

    private UiScale() {}

    public static float scale() { return SCALE; }

    /** Scale a pixel count to the current screen DPI. */
    public static int px(int base) { return Math.round(base * SCALE); }

    /** Scaled preferred size helper. */
    public static Dimension dim(int w, int h) { return new Dimension(px(w), px(h)); }

    /**
     * Apply scaled font sizes to every UIManager default. Call once on the EDT,
     * after the L&amp;F has been installed. Safe to call multiple times — the
     * {@code applied} flag guards against double-scaling.
     *
     * <p>{@link FontUIResource} (not plain {@link Font}) is required so the L&amp;F
     * treats these as defaults rather than user overrides — without it Nimbus and
     * system L&amp;Fs will ignore the replacement.
     */
    public static synchronized void applyToUIManager() {
        if (applied) return;
        applied = true;
        if (SCALE <= 1.01f) return; // no-op on standard-DPI
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof Font font) {
                float newSize = font.getSize2D() * SCALE;
                UIManager.put(key, new FontUIResource(font.deriveFont(newSize)));
            }
        }
    }
}
