package net.mehvahdjukaar.polytone.common.codec_ui.example;

/**
 * Compatibility shim: existing callers ({@code PolytoneForge} / {@code PolytoneFabric})
 * still invoke {@code FooExampleTrigger.open()}. Routes through {@link ExamplesLauncher}
 * which now lists every example schema.
 */
public final class FooExampleTrigger {
    private FooExampleTrigger() {}

    public static void open() {
        ExamplesLauncher.open();
    }
}
