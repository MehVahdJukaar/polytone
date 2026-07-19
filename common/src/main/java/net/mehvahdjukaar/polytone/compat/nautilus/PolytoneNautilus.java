package net.mehvahdjukaar.polytone.compat.nautilus;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.codecui.Schema;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.nautilus.NautilusStudioApi;
import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiICons;
import net.mehvahdjukaar.nautilus.swing.widget.ExpressionWidget;
import net.mehvahdjukaar.nautilus.workbench.CodecEntry;
import net.mehvahdjukaar.nautilus.workbench.PackWorkspace;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.BiomeScenePreview;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.ColormapPreview;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.GuiModifierPreviewPanel;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.NoisePreview;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.ParticlePreview;
import net.mehvahdjukaar.polytone.content.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.content.colormap.ColormapColorModulatorExpression;
import net.mehvahdjukaar.polytone.content.colormap.ColormapExpressionProvider;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.BlockExp;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.ColormapExp;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.ColormapModExp;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.EntityExp;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.LightmapExp;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.PackMetadataExp;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.ParticleExp;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.SimpleExp;
import net.mehvahdjukaar.polytone.content.lightmap.LightmapContextExpression;
import net.mehvahdjukaar.polytone.content.particle.ParticleContextExpression;
import net.mehvahdjukaar.polytone.utils.ContentManager;
import net.mehvahdjukaar.polytone.utils.exp.PolytoneExpression;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class PolytoneNautilus {

    private static final String WIKI_BASE = "https://github.com/MehVahdJukaar/polytone/wiki/";

    // Special preview panels keyed by content folder; attached to the matching CodecEntry as it's built.
    private static final Map<String, TabPreview.Factory> PREVIEWS = Map.of(
            "colormaps", ColormapPreview::new,
            "gui_modifiers", GuiModifierPreviewPanel::new,
            "custom_particles", ParticlePreview::new,
            "noises", NoisePreview::new,
            "biome_modifiers", BiomeScenePreview::new);

    // Bundled Nautilus icons reused by content folder - only the types with an obvious glyph;
    // the rest fall back to Nautilus' generic content icon.
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
            Map.entry("custom_models", "box"),
            Map.entry("custom_item_models", "box"),
            Map.entry("dimension_modifiers", "globe"));

    public static void init() {
        // Widget bindings must exist before any schema resolves (companion registrations only).
        registerWidgetBindings();

        ContentManager.editorWorkspaceJsonLookup = PolytoneNautilus::workspaceContentJson;

        for (ContentManager<?, ?> manager : ContentManager.REGISTRY) {
            Codec<?> codec = manager.contentCodec();
            if (codec == null) continue; // not editable (no file codec)
            // Only the canonical folder, never the legacy aliases (e.g. block_modifiers, not block_properties),
            // so a content type shows up once, not once per legacy parsing folder.
            String folder = manager.primaryFolder();
            if (folder == null) continue;

            // Nautilus derives the schema from the raw codec itself - Polytone doesn't need to build it.
            CodecEntry entry = new CodecEntry(manager.name, "Polytone", SchemaCodec.wrap(codec), Side.CLIENT_RESOURCES,
                    Polytone.MOD_ID + "/" + folder);

            // Content types with companion textures (colormaps, block/fluid/particle tints) show their
            // sibling .png files in the editor via the SAME naming contract the reload driver uses.
            if (manager.contentTexture != null) {
                entry = entry.withSidecars(TextureSidecars.of(manager.contentTexture, Side.CLIENT_RESOURCES));
            }

            String page = manager.wikiPage();
            if (page != null) entry = entry.withWikiUrl(WIKI_BASE + page);

            String icon = ICONS.get(folder);
            if (icon != null) entry = entry.withIcon(UiICons.content(icon));

            TabPreview.Factory preview = PREVIEWS.get(folder);
            if (preview != null) entry = entry.withPreview(preview);

            NautilusStudioApi.register(entry);
        }
    }

    private static void registerWidgetBindings() {
        // ---- MVEL expressions (the current system): one binding per PolyExpType leaf.
        SchemaCodecs.registerCompanion(ColormapExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(ColormapExp.TYPE)));
        SchemaCodecs.registerCompanion(ColormapModExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(ColormapModExp.TYPE)));
        SchemaCodecs.registerCompanion(BlockExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(BlockExp.TYPE)));
        SchemaCodecs.registerCompanion(SimpleExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(SimpleExp.TYPE)));
        SchemaCodecs.registerCompanion(EntityExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(EntityExp.TYPE)));
        SchemaCodecs.registerCompanion(ParticleExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(ParticleExp.TYPE)));
        SchemaCodecs.registerCompanion(LightmapExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(LightmapExp.TYPE)));
        SchemaCodecs.registerCompanion(PackMetadataExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(PackMetadataExp.TYPE)));

        // ---- Legacy exp4j expressions: same editor, exp4j variable chips.
        SchemaCodecs.registerCompanion(ColormapExpressionProvider.CODEC,
                new Schema.Custom<>(exp4jEditor(ColormapExpressionProvider.CODEC, "state_prop",
                        "BIOME_VALUE", "DAMAGE")));
        SchemaCodecs.registerCompanion(ColormapColorModulatorExpression.Exp.CODEC,
                new Schema.Custom<>(exp4jEditor(ColormapColorModulatorExpression.Exp.CODEC, "state_prop",
                        "BIOME_VALUE", "DAMAGE", "RED", "GREEN", "BLUE", "ALPHA")));
        SchemaCodecs.registerCompanion(BlockContextExpression.CODEC,
                new Schema.Custom<>(exp4jEditor(BlockContextExpression.CODEC, "state_prop")));
        SchemaCodecs.registerCompanion(ParticleContextExpression.CODEC,
                new Schema.Custom<>(exp4jEditor(ParticleContextExpression.CODEC, null,
                        "COLOR", "SPEED", "X", "Y", "Z", "DX", "DY", "DZ", "RED", "GREEN", "BLUE",
                        "ALPHA", "SIZE", "LIFETIME", "AGE", "ROLL", "CUSTOM")));
        // Standalone exp4j flavor with its own tiny variable set (not PolytoneExpression-based).
        SchemaCodecs.registerCompanion(LightmapContextExpression.CODEC,
                new Schema.Custom<>(ExpressionWidget.define()
                        .variables("TIME", "RAIN", "THUNDER", "TEMPERATURE", "DOWNFALL")
                        .validator(compileCheck(LightmapContextExpression.CODEC))));
    }

    /** Expression editor for an MVEL {@link PolyExpType}: input chips + real compile check. */
    private static ExpressionWidget.Def mvelEditor(PolyExpType<?> type) {
        return ExpressionWidget.define()
                .variables(type.inputNames().toArray(String[]::new))
                .validator(compileCheck(type.codec()));
    }

    /**
     * Expression editor for an exp4j {@link PolytoneExpression} codec: the family's base
     * variables plus the flavor's own extras, compile-check through the codec itself.
     */
    private static ExpressionWidget.Def exp4jEditor(Codec<?> codec, @Nullable String function,
                                                    String... extraVars) {
        ExpressionWidget.Def def = ExpressionWidget.define()
                .variables(PolytoneExpression.baseVariableNames())
                .variables(extraVars)
                .validator(compileCheck(codec));
        return function != null ? def.functions(function) : def;
    }

    /** Widget validator that parses the raw text through the expression codec itself. */
    private static ExpressionWidget.Validator compileCheck(Codec<?> codec) {
        return text -> {
            if (text.isBlank()) return "empty expression";
            return codec.parse(JsonOps.INSTANCE, new JsonPrimitive(text))
                    .error().map(DataResult.Error::message).orElse(null);
        };
    }

    /**
     * The raw json of {@code assets/<ns>/polytone/<folder>/<path>.json} inside the pack currently
     * open in the editor, or null. Only answers on the editor's own (AWT) thread so an in-game
     * reload can never accidentally resolve a reference against the edited pack.
     */
    private static @Nullable JsonElement workspaceContentJson(String folder, ResourceLocation id) {
        if (!SwingUtilities.isEventDispatchThread()) return null;
        PackWorkspace workspace = NautilusStudioApi.currentWorkspace();
        if (workspace == null) return null;
        Path file = workspace.fileFor(Side.CLIENT_RESOURCES, id.getNamespace(),
                Polytone.MOD_ID + "/" + folder, id.getPath());
        if (!Files.isRegularFile(file)) return null;
        try {
            return JsonParser.parseString(Files.readString(file));
        } catch (Exception e) {
            return null;
        }
    }

    public static void open() {
        NautilusStudioApi.openEditor();
    }

    public static boolean isOpen() {
        return NautilusStudioApi.isOpen();
    }

    public static void close() {
        NautilusStudioApi.close();
    }

}
