package net.mehvahdjukaar.polytone.compat.nautilus;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.codecui.Schema;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.codecui.SchemaHandler;
import net.mehvahdjukaar.nautilus.NautilusStudioApi;
import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.widget.ExpressionWidget;
import net.mehvahdjukaar.nautilus.workbench.CodecEntry;
import net.mehvahdjukaar.nautilus.workbench.SiblingSidecar;
import net.mehvahdjukaar.nautilus.workbench.SidecarAssets;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.PolytoneModelCodecs;
import net.mehvahdjukaar.polytone.common.companion.ContentTextures;
import net.mehvahdjukaar.polytone.common.companion.TextureSlot;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.BiomeScenePreview;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.ColormapPreview;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.CreativeTabPreviewPanel;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.GuiModifierPreviewPanel;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.NoisePreview;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.ParticlePreview;
import net.mehvahdjukaar.polytone.common.exp.PolytoneExpression;
import net.mehvahdjukaar.polytone.common.exp.impl.BlockContextExpression;
import net.mehvahdjukaar.polytone.common.exp.impl.ColormapModContextExpression;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.common.expressions.impl.BlockExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.ColormapExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.ColormapModExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.SimpleExp;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.content.colormap.ColormapExpressionProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class PolytoneNautilus {

    private static final Map<String, TabPreview.Factory> PREVIEWS = Map.of(
            "colormaps", ColormapPreview::new,
            "noises", NoisePreview::new,
            "gui_modifiers", GuiModifierPreviewPanel::new,
            // biome_modifiers: the diorama can't draw its blocks on 26.1 yet, see BiomeSceneRenderPass#drawBlocks
            "custom_particles", ParticlePreview::new,
            "creative_tab_modifiers", CreativeTabPreviewPanel::new);

    private static final String WIKI_BASE = "https://github.com/MehVahdJukaar/polytone/wiki/";

    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("colormaps", "palette"),
            Map.entry("biome_modifiers", "trees"),
            Map.entry("block_modifiers", "blocks"),
            Map.entry("custom_block_sets", "boxes"),
            Map.entry("fluid_modifiers", "droplet"),
            Map.entry("lightmaps", "sun"),
            Map.entry("noises", "audio-waveform"),
            Map.entry("global_expressions", "square-function"),
            Map.entry("custom_sound_types", "volume-2"),
            Map.entry("custom_particles", "sparkles"),
            Map.entry("creative_tab_modifiers", "grid-2x2"),
            Map.entry("dimension_modifiers", "globe"));

    public static void init() {
        // Widget bindings must exist before any schema resolves (companion registrations only).
        registerWidgetBindings();
        registerContentEntries();
        BedrockImports.register();
        NautilusEnvironment.register();
    }

    public static void open() {
        NautilusStudioApi.openEditor();
    }

    public static boolean isOpen() {
        return NautilusStudioApi.isOpen();
    }

    // Every manager is a Polytone reload listener scanning a polytone/ folder, so its entries are grouped
    // "Polytone" regardless of what the codec decodes into. Only codec-backed managers with a scannable
    // container dir are editable; the rest (legacy/WIP) are skipped. A manager that reads several folder
    // names is one content type, so the extra names go on as aliases rather than as their own entries.
    private static void registerContentEntries() {
        for (ContentManager<?> manager : Polytone.MANAGERS) {
            List<String> folders = manager.folderNames();
            if (manager.contentCodec() == null || folders.isEmpty()) continue;
            String folder = folders.getFirst();
            ContentTextures<?> companions = manager.contentTexture;
            String wikiPage = manager.wikiPage();
            NautilusStudioApi.register(new CodecEntry(manager.name, "Polytone",
                    manager.contentCodec(), Side.CLIENT_RESOURCES, Polytone.MOD_ID + "/" + folder)
                    .withAliasDirs(aliasDirs(folders))
                    .withSidecars(companions == null ? null : sidecarsFromSpec(companions))
                    .withPreview(PREVIEWS.get(folder))
                    .withWikiUrl(wikiPage == null ? null : WIKI_BASE + wikiPage)
                    .withIcon(ICONS.get(folder)));
        }
    }

    private static String[] aliasDirs(List<String> folders) {
        return folders.stream().skip(1).map(f -> Polytone.MOD_ID + "/" + f).toArray(String[]::new);
    }

    // Schema companions for codecs that can't carry their schema where they're declared, because a Swing
    // widget must never be referenced from content code. Anything not involving a widget belongs on the
    // codec's own declaration (SchemaRecord / SchemaCodecs.alt) instead.
    private static void registerWidgetBindings() {
        bindMvel(ColormapExp.TYPE);
        bindMvel(BlockExp.TYPE);
        bindMvel(SimpleExp.TYPE);
        // The color-modifier channels (red/green/blue) are their own leaf codec, so they need their own
        // binding to get the same picker colormap's x/y axes have.
        bindMvel(ColormapModExp.TYPE);

        // legacy exp4j expressions: same editor, exp4j variable chips
        bindExp4j(ColormapExpressionProvider.CODEC, "state_prop", "BIOME_VALUE", "DAMAGE");
        bindExp4j(BlockContextExpression.CODEC, null);
        bindExp4j(ColormapModContextExpression.CODEC, "state_prop",
                "BIOME_VALUE", "DAMAGE", "RED", "GREEN", "BLUE", "ALPHA");

        // The variant model-state codec is wrapped opaquely by VariantDeserializerMixin (it merges
        // Polytone's offset/float-rotation keys onto the vanilla one), so codecui can't introspect it
        // and blockstates fall back to raw JSON. Match that exact wrapped instance and hand codecui a
        // flat shape (EDITOR_SHAPE) so it renders a proper form. Lazy: WRAPPED is set by the time a
        // blockstate resolves, and if it isn't we just pass and keep the raw-JSON fallback.
        SchemaCodecs.registerHandler(new SchemaHandler() {
            @Override
            public @Nullable Schema<?> tryResolve(Codec<?> codec, Resolver resolver) {
                return null;
            }

            @Override
            public @Nullable Schema<?> tryResolveMap(MapCodec<?> codec, Resolver resolver) {
                MapCodec<?> wrapped = PolytoneModelCodecs.WRAPPED;
                return wrapped != null && codec == wrapped ? resolver.resolveMap(PolytoneModelCodecs.EDITOR_SHAPE) : null;
            }
        });
    }

    private static void bindMvel(PolyExpType<?> type) {
        ExpressionWidget.define()
                .variables(type.inputNames().toArray(String[]::new))
                .validator(type.codec())
                .bindTo(type.codec());
    }

    private static void bindExp4j(Codec<?> codec, @Nullable String function, String... extraVars) {
        ExpressionWidget.Def def = ExpressionWidget.define()
                .variables(PolytoneExpression.baseVariableNames())
                .variables(extraVars)
                .validator(codec);
        if (function != null) def.functions(function);
        def.bindTo(codec);
    }

    // Projects a content type's ContentTextures onto the json's sibling directory: the expected .png names
    // in declaration order, then whatever else in the folder the naming convention still ties to the stem.
    private static SidecarAssets sidecarsFromSpec(ContentTextures<?> spec) {
        return SidecarAssets.siblings(Side.CLIENT_RESOURCES, ".png",
                (parsedValue, stem) -> expectedSlotsUnchecked(spec, parsedValue, stem).stream()
                        .map(slot -> new SiblingSidecar.Expected(slot.acceptedNames(), slot.label(),
                                slot.required(), slot.remoteLocation()))
                        .toList(),
                spec::roleLabel);
    }

    // The editor decodes each json to an untyped Object and holds textures as ContentTextures<?>, so the
    // value type isn't known statically here. Runtime callers pass a typed value.
    @SuppressWarnings("unchecked")
    private static List<TextureSlot> expectedSlotsUnchecked(ContentTextures<?> spec, @Nullable Object parsedValue,
                                                            String stem) {
        return ((ContentTextures<Object>) spec).expectedSlots(parsedValue, stem);
    }
}
