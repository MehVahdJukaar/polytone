package net.mehvahdjukaar.polytone.content.colormap;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.companion.CompanionSlot;
import net.mehvahdjukaar.polytone.companion.CompanionSpec;
import net.mehvahdjukaar.polytone.companion.TintedTextures;
import net.mehvahdjukaar.polytone.companion.TrackedTextures;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * The colormap-texture ↔ content contract, in ONE place and with ONE path: each content
 * type's {@link CompanionSpec} (built by {@link #singleTexture}/{@link #groupedTexture},
 * declared in the owning manager's constructor and read back via
 * {@code ContentManager.companions}) enumerates <em>bound</em> slots - accepted file names
 * plus the inline {@link Colormap} that receives the texture, or a {@code texture_path}
 * remote location. The editor renders that slot list; the reload-time driver ({@link #fill})
 * walks the SAME list against the scanned textures.
 * There is no second, imperative fill path to drift from - names resolve directly against raw
 * texture ids via {@link TintedTextures}.
 */
public final class ColormapTextures {

    // -------------------- specs (the contract) --------------------

    /**
     * Content associated with exactly ONE texture, {@code <stem><suffix>.png} - a standalone
     * colormap or a modifier with an inline colormap named after it (fluid, particle: empty
     * suffix; item bar color: {@code "_bar"}). Tint-suffixed siblings are NOT associated: at
     * reload they become separate auto-generated content. No colormap in the parsed value →
     * an optional unbound slot (the manager auto-attaches a default-shaped colormap when the
     * texture exists); a reference/expression colormap expects nothing; {@code texture_path}
     * becomes a remote slot.
     */
    public static <V> CompanionSpec<V> singleTexture(Function<V, @Nullable Object> colormapGetter,
                                                     String suffix, String label) {
        return new CompanionSpec<>() {
            @Override
            public @Nullable String classify(String fileName, String stem) {
                return fileName.equalsIgnoreCase(stem + suffix + ".png") ? label : null;
            }

            @Override
            public List<CompanionSlot> expectedSlots(@Nullable V parsedValue, String stem) {
                Object col = parsedValue == null ? null : colormapGetter.apply(parsedValue);
                if (col == null) return List.of(CompanionSlot.optional(label, stem + suffix + ".png"));
                if (col instanceof Colormap c) return colormapSlots(c, stem + suffix, label);
                return List.of(); // reference / expression / compound - no associated texture
            }
        };
    }

    /**
     * Content whose colormap may be an {@link IndexCompoundColorGetter} (block modifiers):
     * {@code <stem>.png} as the default plus {@code <stem>_<tint>.png} per tint index. Tint 0
     * (or a lone compound entry) also accepts the default texture as a fallback.
     */
    public static <V> CompanionSpec<V> groupedTexture(Function<V, @Nullable Object> colormapGetter) {
        return new CompanionSpec<>() {
            @Override
            public @Nullable String classify(String fileName, String stem) {
                Integer tint = TintedTextures.tintIndexOf(fileName, stem);
                return tint == null ? null : TintedTextures.label(tint);
            }

            @Override
            public List<CompanionSlot> expectedSlots(@Nullable V parsedValue, String stem) {
                Object col = parsedValue == null ? null : colormapGetter.apply(parsedValue);
                String defaultLabel = TintedTextures.label(TintedTextures.DEFAULT_INDEX);
                switch (col) {
                    case null -> {
                        return List.of(CompanionSlot.optional(defaultLabel,
                                TintedTextures.fileName(stem, TintedTextures.DEFAULT_INDEX)));
                    }
                    case Colormap c -> {
                        return colormapSlots(c, stem, defaultLabel);
                    }
                    case IndexCompoundColorGetter compound -> {
                        List<CompanionSlot> slots = new ArrayList<>();
                        var getters = compound.getGetters();
                        int[] indices = getters.keySet().toIntArray();
                        Arrays.sort(indices);
                        for (int index : indices) {
                            if (!(getters.get(index) instanceof Colormap inner) || !inner.needsToFillTexture())
                                continue;
                            ResourceLocation explicit = inner.getExplicitTargetTexture();
                            String base = explicit != null ? lastSegment(explicit.getPath()) : stem;
                            List<String> names = new ArrayList<>(2);
                            names.add(TintedTextures.fileName(base, index));
                            // tint 0 / a lone entry falls back to the default texture
                            if (getters.size() == 1 || index == 0) {
                                names.add(TintedTextures.fileName(base, TintedTextures.DEFAULT_INDEX));
                            }
                            slots.add(new CompanionSlot(names, TintedTextures.label(index), true,
                                    inner, explicit == null ? null : explicit.toString()));
                        }
                        return slots;
                    }
                    default -> {
                    }
                }
                return List.of();
            }
        };
    }

    private static List<CompanionSlot> colormapSlots(Colormap c, String siblingBase, String label) {
        if (!c.needsToFillTexture()) return List.of();
        ResourceLocation explicit = c.getExplicitTargetTexture();
        if (explicit != null) {
            return List.of(new CompanionSlot(List.of(lastSegment(explicit.getPath()) + ".png"),
                    "texture_path", true, c, explicit.toString()));
        }
        return List.of(new CompanionSlot(List.of(siblingBase + ".png"), label, true, c, null));
    }

    // -------------------- runtime driver (reload) --------------------

    /**
     * THE reload-time association: walks {@code spec.expectedSlots} for this content instance
     * and fills each bound, unfilled colormap from the scanned textures - accepted names
     * resolve in order against ids in the content's own directory (or the slot's
     * {@code texture_path} location). Bound ids are marked used on {@code textures};
     * {@code strict} = throw when a required slot stays empty.
     */
    public static <V> void fill(CompanionSpec<V> spec, TrackedTextures textures, ResourceLocation contentId,
                                @Nullable V parsedValue, boolean strict) {
        for (CompanionSlot slot : spec.expectedSlots(parsedValue, lastSegment(contentId.getPath()))) {
            if (!(slot.target() instanceof Colormap colormap) || !colormap.needsToFillTexture()) continue;
            ResourceLocation baseId = slot.remoteLocation() != null ? ResourceLocation.parse(slot.remoteLocation()) : contentId;
            String dir = dirOf(baseId.getPath());

            ResourceLocation foundId = null;
            ArrayImage found = null;
            for (String name : slot.acceptedNames()) {
                ResourceLocation candidate = baseId.withPath(dir + stripPng(name));
                ArrayImage img = textures.get(candidate);
                if (img != null) {
                    foundId = candidate;
                    found = img;
                    break;
                }
            }

            if (found != null) {
                fillDirect(textures, foundId, found, colormap);
            } else {
                if (slot.remoteLocation() != null) {
                    Polytone.LOGGER.error("Could not resolve explicit texture at location {}.png. Skipping",
                            slot.remoteLocation());
                }
                if (strict && slot.required()) {
                    throw new IllegalStateException("Could not find any texture .png for slot '" + slot.label()
                            + "' of " + contentId + ". Expected " + baseId.withPath(dir + stripPng(slot.canonicalName())));
                }
            }
        }
    }

    public static void fillDirect(TrackedTextures textures, ResourceLocation textureId, ArrayImage texture,
                                  Colormap colormap) {
        if (!colormap.needsToFillTexture()) return;
        if (texture.pixels().length == 0) {
            throw new IllegalStateException("Colormap texture at location " + textureId + " had invalid 0 dimension");
        }
        colormap.acceptTexture(texture);
        textures.markUsed(textureId);
        colormap.debugID = textureId;
    }

    /**
     * Whether any texture this content COULD use by convention exists - the managers'
     * "no colormap declared, but a texture is there → auto-attach a default" check, asked
     * value-agnostically (permissive slots).
     */
    public static boolean hasUsableTexture(CompanionSpec<?> spec, TrackedTextures textures,
                                           ResourceLocation contentId) {
        String dir = dirOf(contentId.getPath());
        for (CompanionSlot slot : spec.expectedSlots(null, lastSegment(contentId.getPath()))) {
            for (String name : slot.acceptedNames()) {
                if (textures.containsKey(contentId.withPath(dir + stripPng(name)))) return true;
            }
        }
        return false;
    }

    public static Set<Integer> usableTintIndices(TrackedTextures textures, ResourceLocation contentId) {
        String dir = dirOf(contentId.getPath());
        String stem = lastSegment(contentId.getPath());
        Set<Integer> out = new TreeSet<>();
        for (ResourceLocation id : textures.keySet()) {
            if (!id.getNamespace().equals(contentId.getNamespace())) continue;
            String path = id.getPath();
            if (!dirOf(path).equals(dir)) continue;
            Integer index = TintedTextures.tintIndexOf(lastSegment(path) + ".png", stem);
            if (index != null) out.add(index);
        }
        return out;
    }

    /**
     * Stem ids of textures nothing consumed - the "orphan textures become default content"
     * pass for tint-grouped content. A stem any of whose textures WAS consumed is skipped
     * entirely (its extra tints don't spawn a second modifier).
     */
    public static List<ResourceLocation> orphanStems(TrackedTextures textures) {
        // bucket every texture's stem: consumed if the texture was claimed, else a stray candidate.
        // a stem is an orphan iff it's a candidate that no sibling texture ever consumed.
        Set<ResourceLocation> consumedStems = new HashSet<>();
        Set<ResourceLocation> orphans = new LinkedHashSet<>();
        for (ResourceLocation id : textures.keySet()) {
            ResourceLocation stem = stemId(id);
            if (textures.isUsed(id)) consumedStems.add(stem);
            else orphans.add(stem);
        }
        orphans.removeAll(consumedStems);
        return List.copyOf(orphans);
    }

    // -------------------- id/name math --------------------

    private static ResourceLocation stemId(ResourceLocation id) {
        String path = id.getPath();
        return id.withPath(dirOf(path) + TintedTextures.parse(lastSegment(path)).stem());
    }

    private static String dirOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash + 1);
    }

    private static String lastSegment(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private static String stripPng(String name) {
        return name.substring(0, name.length() - ".png".length());
    }
}
