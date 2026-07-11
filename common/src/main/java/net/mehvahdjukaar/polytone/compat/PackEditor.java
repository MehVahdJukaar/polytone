package net.mehvahdjukaar.polytone.compat;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.codecui.Schema;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.nautilus.NautilusStudioApi;
import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.nautilus.swing.widget.ExpressionWidget;
import net.mehvahdjukaar.nautilus.workbench.CodecEntry;
import net.mehvahdjukaar.nautilus.workbench.FileNames;
import net.mehvahdjukaar.nautilus.workbench.PackWorkspace;
import net.mehvahdjukaar.nautilus.workbench.SidecarAssets;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.companion.CompanionSlot;
import net.mehvahdjukaar.polytone.common.companion.CompanionSpec;
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

/**
 * Polytone's integration with the standalone <b>PackEditor</b> mod: the ONE class that knows both
 * worlds. It registers Polytone's widget bindings, companion-asset rules and content codecs with
 * the editor's public API, then delegates open/close to it. The vanilla Minecraft entries and the
 * game-service {@code GameHost} are provided by PackEditor itself — Polytone only contributes its
 * own content.
 *
 * <p>Everything here touches Nautilus Studio classes, so callers MUST guard on the {@code nautilus_studio}
 * mod being loaded (see {@code EditorButton} and the {@code Polytone.init} hook). With the mod
 * absent this class is never referenced and the in-game editor button grays out — nothing loads.</p>
 */
public final class PackEditor {

    /**
     * Register Polytone's widget bindings and content codecs with PackEditor. Called once from
     * {@code Polytone.init} when the {@code nautilus_studio} mod is present.
     */
    public static void init() {
        // Widget bindings must exist before any schema resolves (companion registrations only).
        registerWidgetBindings();
        registerContentEntries();
    }

    /** Open (or focus) the editor window. Any thread. */
    public static void open() {
        NautilusStudioApi.openEditor();
    }

    /** Whether the editor window is currently open. Any thread. */
    public static boolean isOpen() {
        return NautilusStudioApi.isOpen();
    }

    /** Close the editor window if open — it is tied to the world and goes with it. Any thread. */
    public static void close() {
        NautilusStudioApi.close();
    }

    // -------------------- Content entries --------------------

    /**
     * Every manager is a Polytone reload listener scanning a {@code polytone/} folder, so its
     * entries are grouped "Polytone" regardless of what the codec decodes into. Only codec-backed
     * managers with a scannable container dir are editable; the rest (legacy/WIP) are skipped.
     * {@code contentCodec()} IS the file codec, so the editor edits exactly what the reload parses.
     */
    private static void registerContentEntries() {
        for (ContentManager<?> manager : Polytone.MANAGERS) {
            if (manager.contentCodec() == null) continue;
            for (String folder : manager.folderNames()) {
                CompanionSpec<?> companions = manager.companions;
                CodecEntry entry = new CodecEntry(manager.name, "Polytone",
                        manager.contentCodec(), Side.CLIENT_RESOURCES,
                        Polytone.MOD_ID + "/" + folder);
                if (companions != null) entry = entry.withSidecars(sidecarsFromSpec(companions));
                NautilusStudioApi.register(entry);
            }
        }
    }

    // -------------------- Widget bindings --------------------

    /**
     * Polytone's schema companions + Swing widget bindings for codecs that can't carry their
     * schema at the declaration site (widget bindings must never leak into content code). The
     * long-term home for a registration is still the codec's own declaration
     * (SchemaRecord / SchemaCodecs.alt) whenever no widget is involved.
     *
     * <p>Union codecs (IColormapExp / IBlockExp / ISimpleExp) are labeled at their declaration
     * sites; here we only bind the big expression editor to the LEAF codecs — the MVEL
     * {@code PolyExpType} codecs (chips from their declared inputs, compile-check through the
     * real parser) and the legacy exp4j ones.</p>
     */
    private static void registerWidgetBindings() {
        // ---- MVEL expressions (the current system): one binding per PolyExpType leaf.
        // Chips come from the type's declared inputs; validation IS the MVEL compiler.
        SchemaCodecs.registerCompanion(ColormapExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(ColormapExp.TYPE)));
        SchemaCodecs.registerCompanion(BlockExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(BlockExp.TYPE)));
        SchemaCodecs.registerCompanion(SimpleExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(SimpleExp.TYPE)));
        // The color-modifier channel expressions (red/green/blue) are their OWN leaf codecs —
        // without their own binding they fell back to a bare string field, unlike the identical
        // picker on colormap's x/y axes (IColormapExp).
        SchemaCodecs.registerCompanion(ColormapModExp.TYPE.codec(),
                new Schema.Custom<>(mvelEditor(ColormapModExp.TYPE)));

        // ---- Legacy exp4j expressions: same editor, exp4j variable chips.
        SchemaCodecs.registerCompanion(ColormapExpressionProvider.CODEC,
                new Schema.Custom<>(exp4jEditor(ColormapExpressionProvider.CODEC, "state_prop",
                        "BIOME_VALUE", "DAMAGE")));
        SchemaCodecs.registerCompanion(BlockContextExpression.CODEC,
                new Schema.Custom<>(exp4jEditor(BlockContextExpression.CODEC, null)));
        // Legacy exp4j color-modifier expression: the colormap vars plus the RGBA channel inputs.
        SchemaCodecs.registerCompanion(ColormapModContextExpression.CODEC,
                new Schema.Custom<>(exp4jEditor(ColormapModContextExpression.CODEC, "state_prop",
                        "BIOME_VALUE", "DAMAGE", "RED", "GREEN", "BLUE", "ALPHA")));
    }

    /** Expression editor for an MVEL {@link PolyExpType}: input chips + real compile check. */
    private static ExpressionWidget.Def mvelEditor(PolyExpType<?> type) {
        return ExpressionWidget.define()
                .variables(type.inputNames().toArray(String[]::new))
                .validator(compileCheck(type.codec()));
    }

    /**
     * Expression editor for an exp4j {@link PolytoneExpression} codec: the family's base
     * variables (straight from {@code PolytoneExpression.buildVars}) plus the flavor's own
     * extras, compile-check through the codec itself.
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

    // -------------------- Companion (sidecar) assets --------------------

    /**
     * Projects a content type's {@link CompanionSpec} (the runtime companion-asset convention,
     * shared with the reloaders) onto the json's sibling directory as the generic
     * {@link SidecarAssets} the editor renders: expected slots are matched (case-insensitively)
     * against the files actually there, in spec order; leftover siblings the spec still classifies
     * as associated come last as {@link SidecarAssets.State#UNUSED}.
     */
    private static SidecarAssets sidecarsFromSpec(CompanionSpec<?> spec) {
        return (jsonFile, pack, parsedValue) -> {
            Path dir = jsonFile.getParent();
            if (dir == null || !Files.isDirectory(dir)) return List.of();
            String stem = FileNames.stem(String.valueOf(jsonFile.getFileName()));

            Map<String, Path> siblings = new LinkedHashMap<>(); // lowercase name -> path
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile).sorted().forEach(p ->
                        siblings.putIfAbsent(String.valueOf(p.getFileName()).toLowerCase(Locale.ROOT), p));
            } catch (IOException e) {
                return List.of();
            }

            List<SidecarAssets.Slot> out = new ArrayList<>();
            Set<Path> consumed = new HashSet<>();
            for (CompanionSlot slot : expectedSlotsUnchecked(spec, parsedValue, stem)) {
                // texture_path slot: lives at an absolute resource location, not next to the json
                if (slot.remoteLocation() != null) {
                    Path remote = resolvePackAsset(pack, slot.remoteLocation());
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
                String label = spec.classify(name, stem);
                if (label != null) out.add(new SidecarAssets.Slot(name, p, SidecarAssets.State.UNUSED, label));
            }
            return List.copyOf(out);
        };
    }

    /**
     * The editor holds specs heterogeneously ({@code CompanionSpec<?>}) and decodes each json
     * to an untyped {@code Object}, so it can't know the spec's value type statically — this is
     * the one boundary where that erasure is unavoidable. Runtime callers pass a typed value.
     */
    @SuppressWarnings("unchecked")
    private static List<CompanionSlot> expectedSlotsUnchecked(CompanionSpec<?> spec, @Nullable Object parsedValue,
                                                              String stem) {
        return ((CompanionSpec<Object>) spec).expectedSlots(parsedValue, stem);
    }

    /**
     * Best-effort lookup of a {@code ns:path} texture inside the opened pack (handles the
     * lenient root that may or may not contain the {@code assets} level). Null when absent —
     * which for a MISSING card can also mean "resolves from another pack or vanilla".
     */
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
