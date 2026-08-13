package net.mehvahdjukaar.polytone.compat;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.codecui.Schema;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.codecui.SchemaHandler;
import net.mehvahdjukaar.nautilus.NautilusStudioApi;
import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiICons;
import net.mehvahdjukaar.nautilus.swing.widget.ExpressionWidget;
import net.mehvahdjukaar.nautilus.workbench.CodecEntry;
import net.mehvahdjukaar.nautilus.workbench.FileNamesUtil;
import net.mehvahdjukaar.nautilus.workbench.PackWorkspace;
import net.mehvahdjukaar.nautilus.workbench.SidecarAssets;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.PolytoneModelCodecs;
import net.mehvahdjukaar.polytone.common.companion.ContentTextures;
import net.mehvahdjukaar.polytone.common.companion.TextureSlot;
import net.mehvahdjukaar.polytone.compat.nautilus.BedrockImports;
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
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

// Polytone's side of the editor integration: registers widget bindings, companion-asset rules and content
// codecs with the editor's API, then delegates open/close to it. Everything here touches Nautilus Studio
// classes, so callers must guard on the nautilus_studio mod being loaded.
public final class PackEditor {

    // Live preview panels keyed by content folder; attached to the matching CodecEntry as it's built.
    private static final Map<String, TabPreview.Factory> PREVIEWS = Map.of(
            "colormaps", ColormapPreview::new,
            "noises", NoisePreview::new,
            "gui_modifiers", GuiModifierPreviewPanel::new,
            "biome_modifiers", BiomeScenePreview::new,
            "custom_particles", ParticlePreview::new,
            "creative_tab_modifiers", CreativeTabPreviewPanel::new);

    // Base of the in-repo wiki; a manager's wikiPage() is appended for its editor entry's help link.
    private static final String WIKI_BASE = "https://github.com/MehVahdJukaar/polytone/wiki/";

    // Editor sidebar icon (nautilus svg name) per content folder.
    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("colormaps", "palette"),
            Map.entry("biome_modifiers", "trees"),
            Map.entry("block_modifiers", "blocks"),
            Map.entry("custom_block_sets", "boxes"),
            Map.entry("fluid_modifiers", "droplet"),
            Map.entry("lightmaps", "sun"),
            Map.entry("noises", "audio-waveform"),
            Map.entry("global_expressions", "square-function"),
            Map.entry("variant_textures", "image"),
            Map.entry("custom_sound_types", "volume-2"),
            Map.entry("custom_particles", "sparkles"),
            Map.entry("creative_tab_modifiers", "grid-2x2"),
            Map.entry("custom_models", "box"),
            Map.entry("custom_item_models", "box"),
            Map.entry("dimension_modifiers", "globe"));

    public static void init() {
        // Widget bindings must exist before any schema resolves (companion registrations only).
        registerWidgetBindings();
        registerContentEntries();
        BedrockImports.register();
    }

    // Open (or focus) the editor window. Any thread.
    public static void open() {
        NautilusStudioApi.openEditor();
    }

    // Whether the editor window is currently open. Any thread.
    public static boolean isOpen() {
        return NautilusStudioApi.isOpen();
    }

    // Close the editor window if open - it is tied to the world and goes with it. Any thread.
    public static void close() {
        NautilusStudioApi.close();
    }

    // Every manager is a Polytone reload listener scanning a polytone/ folder, so its entries are grouped
    // "Polytone" regardless of what the codec decodes into. Only codec-backed managers with a scannable
    // container dir are editable; the rest (legacy/WIP) are skipped.
    private static void registerContentEntries() {
        for (ContentManager<?> manager : Polytone.MANAGERS) {
            if (manager.contentCodec() == null) continue;
            for (String folder : manager.folderNames()) {
                ContentTextures<?> companions = manager.contentTexture;
                CodecEntry entry = new CodecEntry(manager.name, "Polytone",
                        manager.contentCodec(), Side.CLIENT_RESOURCES,
                        Polytone.MOD_ID + "/" + folder);
                if (companions != null) entry = entry.withSidecars(sidecarsFromSpec(companions));
                TabPreview.Factory preview = PREVIEWS.get(folder);
                if (preview != null) entry = entry.withPreview(preview);
                String wikiPage = manager.wikiPage();
                if (wikiPage != null) entry = entry.withWikiUrl(WIKI_BASE + wikiPage);
                String icon = ICONS.get(folder);
                if (icon != null) entry = entry.withIcon(UiICons.content(icon));
                NautilusStudioApi.register(entry);
            }
        }
    }

    // Polytone's schema companions + Swing widget bindings for codecs that can't carry their schema at the
    // declaration site (widget bindings must never leak into content code). The long-term home for a
    // registration is still the codec's own declaration (SchemaRecord / SchemaCodecs.alt) whenever no widget
    // is involved.
    private static void registerWidgetBindings() {
        // ---- MVEL expressions (the current system): one binding per PolyExpType leaf.
        // Chips come from the type's declared inputs; validation IS the MVEL compiler.
        SchemaCodecs.registerCompanion(ColormapExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(ColormapExp.TYPE)));
        SchemaCodecs.registerCompanion(BlockExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(BlockExp.TYPE)));
        SchemaCodecs.registerCompanion(SimpleExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(SimpleExp.TYPE)));
        // The color-modifier channel expressions (red/green/blue) are their OWN leaf codecs - // without their own binding they fell back to a bare string field, unlike the identical
        // picker on colormap's x/y axes (IColormapExp).
        SchemaCodecs.registerCompanion(ColormapModExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(ColormapModExp.TYPE)));

        // legacy exp4j expressions: same editor, exp4j variable chips
        SchemaCodecs.registerCompanion(ColormapExpressionProvider.CODEC,
                new Schema.Custom<>(exp4jEditor(ColormapExpressionProvider.CODEC, "state_prop",
                        "BIOME_VALUE", "DAMAGE")));
        SchemaCodecs.registerCompanion(BlockContextExpression.CODEC,
                new Schema.Custom<>(exp4jEditor(BlockContextExpression.CODEC, null)));
        // Legacy exp4j color-modifier expression: the colormap vars plus the RGBA channel inputs.
        SchemaCodecs.registerCompanion(ColormapModContextExpression.CODEC,
                new Schema.Custom<>(exp4jEditor(ColormapModContextExpression.CODEC, "state_prop",
                        "BIOME_VALUE", "DAMAGE", "RED", "GREEN", "BLUE", "ALPHA")));

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

    private static ExpressionWidget.Def mvelEditor(PolyExpType<?> type) {
        return ExpressionWidget.define()
                .variables(type.inputNames().toArray(String[]::new))
                .validator(compileCheck(type.codec()));
    }

    // Expression editor for an exp4j PolytoneExpression codec: the family's base variables
    // (straight from PolytoneExpression.buildVars) plus the flavor's own extras, compile-check
    // through the codec itself.
    private static ExpressionWidget.Def exp4jEditor(Codec<?> codec, @Nullable String function,
                                                    String... extraVars) {
        ExpressionWidget.Def def = ExpressionWidget.define()
                .variables(PolytoneExpression.baseVariableNames())
                .variables(extraVars)
                .validator(compileCheck(codec));
        return function != null ? def.functions(function) : def;
    }

    private static ExpressionWidget.Validator compileCheck(Codec<?> codec) {
        return text -> {
            if (text.isBlank()) return "empty expression";
            return codec.parse(JsonOps.INSTANCE, new JsonPrimitive(text))
                    .error().map(DataResult.Error::message).orElse(null);
        };
    }

    // Projects a content type's ContentTextures (the runtime companion-asset convention, shared with the
    // reloaders) onto the json's sibling directory as the generic SidecarAssets the editor renders: expected
    // slots are matched (case-insensitively) against the files actually there, in declaration order; leftover
    // siblings the naming convention still associates with the stem come last as SidecarAssets.State#UNUSED.
    private static SidecarAssets sidecarsFromSpec(ContentTextures<?> spec) {
        return (jsonFile, pack, parsedValue) -> {
            Path dir = jsonFile.getParent();
            if (dir == null || !Files.isDirectory(dir)) return List.of();
            String stem = FileNamesUtil.stem(String.valueOf(jsonFile.getFileName()));

            Map<String, Path> siblings = new LinkedHashMap<>(); // lowercase name -> path
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile).sorted().forEach(p ->
                        siblings.putIfAbsent(String.valueOf(p.getFileName()).toLowerCase(Locale.ROOT), p));
            } catch (IOException e) {
                return List.of();
            }

            List<SidecarAssets.Slot> out = new ArrayList<>();
            Set<Path> consumed = new HashSet<>();
            for (TextureSlot slot : expectedSlotsUnchecked(spec, parsedValue, stem)) {
                // texture_path slot: lives at an absolute resource location, not next to the json
                if (slot.remoteLocation() != null) {
                    Path remote = resolvePackAsset(pack, slot.remoteLocation().toString());
                    String display = slot.remoteLocation() + ".png";
                    out.add(remote != null
                            ? new SidecarAssets.Slot(display, remote, SidecarAssets.State.PRESENT, slot.label())
                            : new SidecarAssets.Slot(display, null, SidecarAssets.State.MISSING, slot.label()));
                    continue;
                }
                Path found = null;
                for (String name : slot.acceptedNames()) {
                    found = siblings.get(name.toLowerCase(Locale.ROOT));
                    if (found != null) break;
                }
                if (found != null) {
                    if (consumed.add(found)) {
                        out.add(new SidecarAssets.Slot(String.valueOf(found.getFileName()), found,
                                SidecarAssets.State.PRESENT, slot.label()));
                    }
                } else if (slot.required()) {
                    out.add(new SidecarAssets.Slot(slot.canonicalName(), null,
                            SidecarAssets.State.MISSING, slot.label()));
                }
            }
            for (Path p : siblings.values()) {
                if (consumed.contains(p)) continue;
                String name = String.valueOf(p.getFileName());
                String label = spec.roleLabel(name, stem);
                if (label != null) out.add(new SidecarAssets.Slot(name, p, SidecarAssets.State.UNUSED, label));
            }
            return List.copyOf(out);
        };
    }

    // The editor holds textures heterogeneously (ContentTextures<?>) and decodes each json to an untyped
    // Object, so it can't know the value type statically - this is the one boundary where that erasure is
    // unavoidable. Runtime callers pass a typed value.
    @SuppressWarnings("unchecked")
    private static List<TextureSlot> expectedSlotsUnchecked(ContentTextures<?> spec, @Nullable Object parsedValue,
                                                            String stem) {
        return ((ContentTextures<Object>) spec).expectedSlots(parsedValue, stem);
    }

    // Best-effort lookup of a ns:path texture inside the opened pack (handles the lenient root that may or may
    // not contain the assets level). Null when absent - which for a MISSING card can also mean "resolves from
    // another pack or vanilla".
    private static @Nullable Path resolvePackAsset(PackWorkspace pack, String location) {
        Identifier id = Identifier.tryParse(location);
        if (id == null) return null;
        for (String prefix : new String[]{"assets/" + id.getNamespace() + "/", id.getNamespace() + "/"}) {
            Path p = pack.root().resolve(prefix + id.getPath() + ".png");
            if (Files.isRegularFile(p)) return p;
        }
        return null;
    }
}
