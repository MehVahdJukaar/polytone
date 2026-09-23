package net.mehvahdjukaar.polytone.common.companion;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.StrUtils;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IndexCompoundColorGetter;
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

public final class ContentTextures<V> {

    public record Orphan<V>(ResourceLocation stemId, Map<TextureRole<V>, Set<Integer>> parts) {
    }

    private final List<TextureRole<V>> parts;
    private final List<TextureRole<V>> byNameSpecificity;

    public ContentTextures(List<TextureRole<V>> roles) {
        if (roles.isEmpty()) throw new IllegalArgumentException("Need at least one texture part");
        this.parts = List.copyOf(roles);
        this.byNameSpecificity = this.parts.stream()
                .sorted(Comparator.comparingInt((TextureRole<V> p) -> p.namePattern().orphanPriority()).reversed())
                .toList();
    }

    private TextureRole<V> mainPart() {
        return parts.getFirst();
    }

    // a null value (json not currently parseable) degrades to possibleSlots
    public List<TextureSlot> expectedSlots(@Nullable V value, String stem) {
        if (value == null) return possibleSlots(stem);
        List<TextureSlot> slots = new ArrayList<>();
        boolean plainFallbackFree = true;
        for (TextureRole<V> part : parts) {
            Object declared = part.getDeclaredColormap(value);
            String canonical = part.namePattern().fileName(stem, FileNamePattern.NO_INDEX);
            if (declared == null) {
                // nothing declared: an unbound slot (managers auto-attach defaults when the texture exists)
                slots.add(TextureSlot.optional(part.displayLabel(), canonical));
                continue;
            }
            switch (declared) {
                case Colormap c -> {
                    List<TextureSlot> built = colormapSlots(c, canonical, part.displayLabel());
                    if (built.isEmpty()) continue;
                    TextureSlot slot = built.getFirst();
                    boolean isPlain = canonical.equalsIgnoreCase(stem + ".png");
                    if (plainFallbackFree && !isPlain && slot.explicitTexturePath() == null) {
                        slots.add(new TextureSlot(List.of(canonical, stem + ".png"),
                                slot.displayLabel(), slot.colormapToFill(), null));
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

    public List<TextureSlot> possibleSlots(String stem) {
        List<TextureSlot> slots = new ArrayList<>();
        boolean plainCovered = false;
        for (TextureRole<V> part : parts) {
            String canonical = part.namePattern().fileName(stem, FileNamePattern.NO_INDEX);
            slots.add(TextureSlot.optional(part.displayLabel(), canonical));
            if (canonical.equalsIgnoreCase(stem + ".png")) plainCovered = true;
        }
        // plain <stem>.png is meaningful even when no part names it outright (fallback rules)
        if (!plainCovered) {
            slots.add(TextureSlot.optional(FileNamePattern.label(FileNamePattern.NO_INDEX), stem + ".png"));
        }
        return slots;
    }

    public void fill(ScannedTextures textures, ResourceLocation contentId, @Nullable V value, boolean strict) {
        String stem = StrUtils.lastSegment(contentId.getPath());
        //example: some/thing -> thing
        for (TextureSlot slot : expectedSlots(value, stem)) {
            Colormap colormap = slot.colormapToFill();
            if (colormap == null || !colormap.needsToFillTexture()) continue;
            ResourceLocation baseId = slot.explicitTexturePath() != null ? slot.explicitTexturePath() : contentId;

            ResourceLocation foundId = slot.findFirstMatch(fileName -> textures.find(baseId, fileName));
            if (foundId != null) {
                textures.fillColormap(foundId, colormap);
            } else {
                if (slot.explicitTexturePath() != null) {
                    Polytone.LOGGER.error("Could not resolve explicit texture at location {}.png. Skipping",
                            slot.explicitTexturePath());
                }
                if (strict) { // a bound slot is always required
                    throw new IllegalStateException("Could not find any texture .png for slot '" + slot.displayLabel()
                            + "' of " + contentId + ". Expected " + slot.canonicalName()
                            + " in directory of " + baseId);
                }
            }
        }
    }

    //dont delete
    public @Nullable String roleLabel(String fileName, String stem) {
        for (TextureRole<V> part : parts) {
            Integer index = part.namePattern().indexOf(fileName, stem);
            if (index != null) return part.namePattern().slotLabel(part.displayLabel(), index);
        }
        // plain <stem>.png always reads as the default even when no part names it outright
        return fileName.equalsIgnoreCase(stem + ".png") ? FileNamePattern.label(FileNamePattern.NO_INDEX) : null;
    }

    public Map<TextureRole<V>, Set<Integer>> adoptable(ScannedTextures textures, ResourceLocation contentId, V value) {
        String stem = StrUtils.lastSegment(contentId.getPath());
        Map<TextureRole<V>, Set<Integer>> out = new LinkedHashMap<>();
        long declaredCount = parts.stream().filter(p -> p.getDeclaredColormap(value) != null).count();
        for (TextureRole<V> part : parts) {
            if (part.getDeclaredColormap(value) != null) continue;
            Set<Integer> indexes = part.namePattern().presentIndexes(textures, contentId);
            if (!indexes.isEmpty()) out.put(part, indexes);
        }
        // a lone plain texture with nothing declared at all: the main feature adopts it. When
        // something IS declared, the plain name is that slot's fallback instead (see expectedSlots)
        if (declaredCount == 0 && !out.containsKey(mainPart())
                && textures.find(contentId, stem + ".png") != null) {
            out.put(mainPart(), Set.of(FileNamePattern.NO_INDEX));
        }
        return out;
    }

    //most specific namePattern first, so a name two parts could claim goes to the narrower one
    public List<Orphan<V>> orphans(ScannedTextures textures, Set<ResourceLocation> contentIds) {
        Map<ResourceLocation, Map<TextureRole<V>, Set<Integer>>> groups = new LinkedHashMap<>();
        Set<ResourceLocation> owned = new HashSet<>();
        for (ResourceLocation id : textures.keySet()) {
            String dir = StrUtils.directoryOf(id.getPath());
            String base = StrUtils.lastSegment(id.getPath());

            TextureRole<V> part = null;
            FileNamePattern.ParsedName name = null;
            for (TextureRole<V> candidate : byNameSpecificity) {
                name = candidate.namePattern().parseName(base);
                if (name != null) {
                    part = candidate;
                    break;
                }
            }
            if (part == null) {
                part = mainPart();
                name = new FileNamePattern.ParsedName(base, FileNamePattern.NO_INDEX);
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

    private List<TextureSlot> indexedColormapSlots(TextureRole<V> part, String stem,
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
                    ? i -> FileNamePattern.tintedFileName(StrUtils.lastSegment(explicit.getPath()), i)
                    : i -> part.namePattern().fileName(stem, i);
            List<String> names = new ArrayList<>(2);
            names.add(name.apply(index));
            // tint 0 / a lone entry falls back to the default texture
            if (getters.size() == 1 || index == 0) {
                names.add(name.apply(FileNamePattern.NO_INDEX));
            }
            slots.add(new TextureSlot(names, part.namePattern().slotLabel(part.displayLabel(), index), inner, explicit));
        }
        return slots;
    }

    private static List<TextureSlot> colormapSlots(Colormap c, String canonicalName, String label) {
        if (!c.needsToFillTexture()) return List.of();
        ResourceLocation explicit = c.getExplicitTargetTexture();
        if (explicit != null) {
            return List.of(TextureSlot.requiredExplicit(c, explicit, "texture_path",
                    StrUtils.lastSegment(explicit.getPath()) + ".png"));
        }
        return List.of(TextureSlot.required(c, label, canonicalName));
    }
}
