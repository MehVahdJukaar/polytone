package net.mehvahdjukaar.polytone.companion;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IndexCompoundColorGetter;
import net.mehvahdjukaar.polytone.utils.StrUtils;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntFunction;

/**
 * The texture half of a content type's contract with the pack: which {@code .png} files belong
 * to a content json (next to it by naming convention, or at a {@code texture_path} location) and
 * which inline {@link Colormap} receives each one. ONE instance per content type, built from the
 * {@link TexturePart}s declared on the manager's {@code Spec}, is the single source of truth:
 * the reload driver ({@link #fill}, {@link #adoptable}, {@link #orphans}) and the editor's
 * sidecar view both walk the same parts with the same {@link TextureSlot#findFirstMatch} rule,
 * so they can never disagree about which files belong to a json.
 *
 * <p>Every companion texture resolves to a {@code (part, tintIndex)} pair; a part's
 * {@link Naming} decides how file names encode that. This class only answers naming questions -
 * constructing the default content that adopts a texture is the manager's job.</p>
 *
 * <p>Forward direction, json stem to expected files: {@link #expectedSlots},
 * {@link #possibleSlots}, {@link #fill}. Backward direction, file back to its role:
 * {@link #roleLabel}, {@link #adoptable}, {@link #orphans}.</p>
 */
public final class ContentTextures<V> {

    /**
     * A leftover-texture group the manager should turn into a default content entry: the content
     * id to create and, per part, the tint indexes its textures cover. Produced by {@link #orphans}.
     */
    public record Orphan<V>(ResourceLocation stemId, Map<TexturePart<V>, Set<Integer>> parts) {
    }

    private final List<TexturePart<V>> parts;             // declaration order; first = main feature
    private final List<TexturePart<V>> byNameSpecificity; // for reverse name parsing

    public ContentTextures(List<TexturePart<V>> parts) {
        if (parts.isEmpty()) throw new IllegalArgumentException("Need at least one texture part");
        this.parts = List.copyOf(parts);
        this.byNameSpecificity = this.parts.stream()
                .sorted(Comparator.comparingInt((TexturePart<V> p) -> p.naming().parseSpecificity()).reversed())
                .toList();
    }

    private TexturePart<V> mainPart() {
        return parts.getFirst();
    }

    // -------------------- forward: json stem -> expected files --------------------

    /**
     * The slots this content actually expects given its parsed value: what {@link #fill} will
     * look up, and error about when a bound slot's texture is absent. A null {@code value}
     * (json not currently parseable) degrades to {@link #possibleSlots}.
     */
    public List<TextureSlot> expectedSlots(@Nullable V value, String stem) {
        if (value == null) return possibleSlots(stem);
        List<TextureSlot> slots = new ArrayList<>();
        // wiki rule: the first part declaring a local inline colormap whose canonical name isn't
        // plain <stem>.png itself additionally accepts the plain name (specific name preferred)
        boolean plainFallbackFree = true;
        for (TexturePart<V> part : parts) {
            Object declared = part.declared(value);
            String canonical = part.naming().fileName(stem, Naming.DEFAULT_INDEX);
            if (declared == null) {
                // nothing declared: an unbound slot (managers auto-attach defaults when the texture exists)
                slots.add(TextureSlot.unbound(part.label(), canonical));
                continue;
            }
            switch (declared) {
                case Colormap c -> {
                    List<TextureSlot> built = colormapSlots(c, canonical, part.label());
                    if (built.isEmpty()) continue;
                    TextureSlot slot = built.getFirst();
                    boolean isPlain = canonical.equalsIgnoreCase(stem + ".png");
                    if (plainFallbackFree && !isPlain && slot.remoteLocation() == null) {
                        slots.add(new TextureSlot(List.of(canonical, stem + ".png"),
                                slot.label(), slot.target(), null));
                    } else {
                        slots.addAll(built);
                    }
                    plainFallbackFree = false;
                }
                case IndexCompoundColorGetter compound -> slots.addAll(indexedColormapSlots(part, stem, compound));
                default -> {
                } // reference / expression: nothing of ours to fill
            }
        }
        return slots;
    }

    /** Every slot the content COULD have, ignoring what its json declares (all unbound). */
    public List<TextureSlot> possibleSlots(String stem) {
        List<TextureSlot> slots = new ArrayList<>();
        boolean plainCovered = false;
        for (TexturePart<V> part : parts) {
            String canonical = part.naming().fileName(stem, Naming.DEFAULT_INDEX);
            slots.add(TextureSlot.unbound(part.label(), canonical));
            if (canonical.equalsIgnoreCase(stem + ".png")) plainCovered = true;
        }
        // plain <stem>.png is meaningful even when no part names it outright (fallback rules)
        if (!plainCovered) {
            slots.add(TextureSlot.unbound(Naming.label(Naming.DEFAULT_INDEX), stem + ".png"));
        }
        return slots;
    }

    /**
     * THE reload-time association: walks {@link #expectedSlots} for this content instance and
     * pours the matching scanned texture into each bound colormap, resolving accepted names in
     * order against the content's own directory (or the slot's {@code texture_path} location).
     * Consumed textures are marked used on {@code textures}; {@code strict} = throw when a
     * bound slot's texture is absent.
     */
    public void fill(TrackedTextures textures, ResourceLocation contentId, @Nullable V value, boolean strict) {
        String stem = StrUtils.lastSegment(contentId.getPath());
        for (TextureSlot slot : expectedSlots(value, stem)) {
            Colormap colormap = slot.target();
            if (colormap == null || !colormap.needsToFillTexture()) continue;
            ResourceLocation baseId = slot.remoteLocation() != null ? slot.remoteLocation() : contentId;

            ResourceLocation foundId = slot.findFirstMatch(fileName -> textures.find(baseId, fileName));
            if (foundId != null) {
                textures.fillColormap(foundId, colormap);
            } else {
                if (slot.remoteLocation() != null) {
                    Polytone.LOGGER.error("Could not resolve explicit texture at location {}.png. Skipping",
                            slot.remoteLocation());
                }
                if (strict) { // a bound slot is always required
                    throw new IllegalStateException("Could not find any texture .png for slot '" + slot.label()
                            + "' of " + contentId + ". Expected " + slot.canonicalName()
                            + " in directory of " + baseId);
                }
            }
        }
    }

    // -------------------- backward: file -> role --------------------

    /**
     * The role {@code fileName} plays for content named {@code stem} - a short display label
     * like {@code "default"} or {@code "tint 3"} - or null when the file is not associated with
     * that stem at all.
     */
    public @Nullable String roleLabel(String fileName, String stem) {
        for (TexturePart<V> part : parts) {
            Integer index = part.naming().indexOf(fileName, stem);
            if (index != null) return part.naming().slotLabel(part.label(), index);
        }
        // plain <stem>.png always reads as the default even when no part names it outright
        return fileName.equalsIgnoreCase(stem + ".png") ? Naming.label(Naming.DEFAULT_INDEX) : null;
    }

    /**
     * The parts of {@code contentId} that have a scanned texture but no declared colormap, with
     * the tint indexes those textures cover - the "auto-attach a default" query. A plain
     * {@code <stem>.png} with nothing declared anywhere counts toward the main (first) part.
     * The manager builds and merges the actual default content for each returned entry.
     */
    public Map<TexturePart<V>, Set<Integer>> adoptable(TrackedTextures textures, ResourceLocation contentId, V value) {
        String stem = StrUtils.lastSegment(contentId.getPath());
        Map<TexturePart<V>, Set<Integer>> out = new LinkedHashMap<>();
        long declaredCount = parts.stream().filter(p -> p.declared(value) != null).count();
        for (TexturePart<V> part : parts) {
            if (part.declared(value) != null) continue;
            Set<Integer> indexes = part.naming().presentIndexes(textures, contentId);
            if (!indexes.isEmpty()) out.put(part, indexes);
        }
        // a lone plain texture with nothing declared at all: the main feature adopts it. When
        // something IS declared, the plain name is that slot's fallback instead (see expectedSlots)
        if (declaredCount == 0 && !out.containsKey(mainPart())
                && textures.find(contentId, stem + ".png") != null) {
            out.put(mainPart(), Set.of(Naming.DEFAULT_INDEX));
        }
        return out;
    }

    /**
     * The leftover (unclaimed) textures grouped under the content id their names encode, most
     * specific naming first; a name no part explains reads as the main part's plain texture.
     * Runs after the per-json pass: any group with a consumed member, or whose id (under either
     * reading of the name) already belongs to a content json, is skipped - orphans are exactly
     * the textures no json accounts for.
     */
    public List<Orphan<V>> orphans(TrackedTextures textures, Set<ResourceLocation> contentIds) {
        Map<ResourceLocation, Map<TexturePart<V>, Set<Integer>>> groups = new LinkedHashMap<>();
        Set<ResourceLocation> owned = new HashSet<>();
        for (ResourceLocation id : textures.keySet()) {
            String dir = StrUtils.directoryOf(id.getPath());
            String base = StrUtils.lastSegment(id.getPath());

            TexturePart<V> part = null;
            Naming.ParsedName name = null;
            for (TexturePart<V> candidate : byNameSpecificity) {
                name = candidate.naming().parseName(base);
                if (name != null) {
                    part = candidate;
                    break;
                }
            }
            if (part == null) {
                part = mainPart();
                name = new Naming.ParsedName(base, Naming.DEFAULT_INDEX);
            }
            ResourceLocation stemId = id.withPath(dir + name.stem());

            if (textures.isUsed(id) || contentIds.contains(stemId) || contentIds.contains(id)) {
                owned.add(stemId);
                continue;
            }
            groups.computeIfAbsent(stemId, k -> new LinkedHashMap<>())
                    .computeIfAbsent(part, k -> new TreeSet<>()).add(name.index());
        }
        return groups.entrySet().stream()
                .filter(e -> !owned.contains(e.getKey()))
                .map(e -> new Orphan<>(e.getKey(), e.getValue()))
                .toList();
    }

    // -------------------- slot building --------------------

    private List<TextureSlot> indexedColormapSlots(TexturePart<V> part, String stem,
                                                   IndexCompoundColorGetter compound) {
        List<TextureSlot> slots = new ArrayList<>();
        var getters = compound.getGetters();
        int[] indices = getters.keySet().toIntArray();
        Arrays.sort(indices);
        for (int index : indices) {
            if (!(getters.get(index) instanceof Colormap inner) || !inner.needsToFillTexture())
                continue;
            ResourceLocation explicit = inner.getExplicitTargetTexture();
            IntFunction<String> name = explicit != null
                    ? i -> Naming.tintedFileName(StrUtils.lastSegment(explicit.getPath()), i)
                    : i -> part.naming().fileName(stem, i);
            List<String> names = new ArrayList<>(2);
            names.add(name.apply(index));
            // tint 0 / a lone entry falls back to the default texture
            if (getters.size() == 1 || index == 0) {
                names.add(name.apply(Naming.DEFAULT_INDEX));
            }
            slots.add(new TextureSlot(names, part.naming().slotLabel(part.label(), index), inner, explicit));
        }
        return slots;
    }

    private static List<TextureSlot> colormapSlots(Colormap c, String canonicalName, String label) {
        if (!c.needsToFillTexture()) return List.of();
        ResourceLocation explicit = c.getExplicitTargetTexture();
        if (explicit != null) {
            return List.of(TextureSlot.fillingRemote(c, explicit, "texture_path",
                    StrUtils.lastSegment(explicit.getPath()) + ".png"));
        }
        return List.of(TextureSlot.filling(c, label, canonicalName));
    }
}
