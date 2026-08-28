package net.mehvahdjukaar.polytone.compat.nautilus;

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
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.BiomeScenePreview;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.ColormapPreview;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.CreativeTabPreviewPanel;
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
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class PolytoneNautilus {

    private static final String WIKI_BASE = "https://github.com/MehVahdJukaar/polytone/wiki/";

    private static final Map<String, TabPreview.Factory> PREVIEWS = Map.of(
            "colormaps", ColormapPreview::new,
            "gui_modifiers", GuiModifierPreviewPanel::new,
            "custom_particles", ParticlePreview::new,
            "noises", NoisePreview::new,
            "biome_modifiers", BiomeScenePreview::new,
            "creative_tab_modifiers", CreativeTabPreviewPanel::new);

    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("colormaps", "palette"),
            Map.entry("biome_modifiers", "trees"),
            Map.entry("block_modifiers", "blocks"),
            Map.entry("custom_block_sets", "boxes"),
            Map.entry("fluid_modifiers", "droplet"),
            Map.entry("lightmaps", "sun"),
            Map.entry("block_lights", "lightbulb"),
            Map.entry("entity_lights", "lightbulb"),
            Map.entry("item_lights", "lightbulb"),
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
        registerWidgetBindings();

        for (ContentManager<?, ?> manager : ContentManager.REGISTRY) {
            Codec<?> codec = manager.contentCodec();
            if (codec == null) continue; // not editable (no file codec)
            // Only the canonical folder, never the legacy aliases (e.g. block_modifiers, not block_properties),
            // so a content type shows up once, not once per legacy parsing folder.
            String folder = manager.primaryFolder();
            if (folder == null) continue;

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
        bindMvel(ColormapExp.TYPE);
        bindMvel(ColormapModExp.TYPE);
        bindMvel(BlockExp.TYPE);
        bindMvel(SimpleExp.TYPE);
        bindMvel(EntityExp.TYPE);
        bindMvel(ParticleExp.TYPE);
        bindMvel(LightmapExp.TYPE);
        bindMvel(PackMetadataExp.TYPE);

        // legacy exp4j expressions: same editor, exp4j variable chips
        bindExp4j(ColormapExpressionProvider.CODEC, "state_prop", "BIOME_VALUE", "DAMAGE");
        bindExp4j(ColormapColorModulatorExpression.Exp.CODEC, "state_prop",
                "BIOME_VALUE", "DAMAGE", "RED", "GREEN", "BLUE", "ALPHA");
        bindExp4j(BlockContextExpression.CODEC, "state_prop");
        bindExp4j(ParticleContextExpression.CODEC, null,
                "COLOR", "SPEED", "X", "Y", "Z", "DX", "DY", "DZ", "RED", "GREEN", "BLUE",
                "ALPHA", "SIZE", "LIFETIME", "AGE", "ROLL", "CUSTOM");
        // Standalone exp4j flavor with its own tiny variable set (not PolytoneExpression-based).
        bind(LightmapContextExpression.CODEC, ExpressionWidget.define()
                .variables("TIME", "RAIN", "THUNDER", "TEMPERATURE", "DOWNFALL")
                .validator(compileCheck(LightmapContextExpression.CODEC)));
    }

    private static void bindMvel(PolyExpType<?> type) {
        bind(type.codec(), ExpressionWidget.define()
                .variables(type.inputNames().toArray(String[]::new))
                .validator(compileCheck(type.codec())));
    }

    private static void bindExp4j(Codec<?> codec, @Nullable String function, String... extraVars) {
        ExpressionWidget.Def def = ExpressionWidget.define()
                .variables(PolytoneExpression.baseVariableNames())
                .variables(extraVars)
                .validator(compileCheck(codec));
        if (function != null) def = def.functions(function);
        bind(codec, def);
    }

    private static <T> void bind(Codec<T> codec, ExpressionWidget.Def def) {
        SchemaCodecs.registerCompanion(codec, new Schema.Custom<>(def));
    }

    private static ExpressionWidget.Validator compileCheck(Codec<?> codec) {
        return text -> {
            if (text.isBlank()) return "empty expression";
            return codec.parse(JsonOps.INSTANCE, new JsonPrimitive(text))
                    .error().map(DataResult.Error::message).orElse(null);
        };
    }

    public static void open() {
        NautilusStudioApi.openEditor();
    }

    public static boolean isOpen() {
        return NautilusStudioApi.isOpen();
    }
}
