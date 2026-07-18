package net.mehvahdjukaar.polytone.compat.nautilus;

import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.nautilus.workbench.PackWorkspace;
import net.mehvahdjukaar.nautilus.workbench.SidecarAssets;
import net.mehvahdjukaar.polytone.companion.ContentTextures;
import net.mehvahdjukaar.polytone.companion.TextureSlot;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Bridges a content type's ContentTextures (the runtime-side texture contract) to the editor's
// SidecarAssets view, so the Nautilus tab shows a colormap's .png textures next to its json: present,
// expected-but-missing, and stray-but-unused. Slot matching goes through the same
// TextureSlot.findFirstMatch rule as the reload driver (ContentTextures.fill), so the two can't drift.
final class TextureSidecars {

    static SidecarAssets of(ContentTextures<?> association, Side side) {
        return (jsonFile, pack, parsedValue) -> {
            try {
                return discover(association, side, jsonFile, pack, parsedValue);
            } catch (Exception e) {
                // discover() must never throw on IO/parse problems - an empty view is the safe fallback
                return List.of();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static List<SidecarAssets.Slot> discover(ContentTextures<?> rawAssociation, Side side,
                                                     Path jsonFile, PackWorkspace pack, Object parsedValue) {
        // nautilus hands parsedValue over untyped; this is the one boundary where V is erased
        ContentTextures<Object> association = (ContentTextures<Object>) rawAssociation;

        Path dir = jsonFile.getParent();
        Path nameP = jsonFile.getFileName();
        if (dir == null || nameP == null) return List.of();
        String fileName = nameP.toString();
        String stem = fileName.toLowerCase(Locale.ROOT).endsWith(".json")
                ? fileName.substring(0, fileName.length() - ".json".length()) : fileName;

        List<Path> siblings = pack.children(dir);
        Map<String, Path> byName = new HashMap<>();
        for (Path p : siblings) {
            Path n = p.getFileName();
            if (n != null && Files.isRegularFile(p)) byName.put(n.toString().toLowerCase(Locale.ROOT), p);
        }

        List<SidecarAssets.Slot> out = new ArrayList<>();
        Set<Path> claimed = new HashSet<>();

        for (TextureSlot slot : association.expectedSlots(parsedValue, stem)) {
            if (slot.remoteLocation() != null) {
                out.add(remoteSlot(slot, pack, side));
                continue;
            }
            Path found = slot.findFirstMatch(name -> byName.get(name.toLowerCase(Locale.ROOT)));
            if (found != null) {
                // a file matched by two slots is emitted once, not as two PRESENT rows
                if (claimed.add(found)) {
                    out.add(new SidecarAssets.Slot(found.getFileName().toString(), found,
                            SidecarAssets.State.PRESENT, slot.label()));
                }
            } else if (slot.required()) {
                // unbound-and-absent slots are fine to omit; only flag bound ones as missing
                out.add(new SidecarAssets.Slot(slot.canonicalName(), null,
                        SidecarAssets.State.MISSING, slot.label()));
            }
        }

        // stray siblings the naming convention associates with this stem but no slot consumed
        for (Path p : siblings) {
            Path n = p.getFileName();
            if (n == null || claimed.contains(p)) continue;
            if (!byName.containsKey(n.toString().toLowerCase(Locale.ROOT))) continue; // not a regular file
            String label = association.roleLabel(n.toString(), stem);
            if (label != null) {
                out.add(new SidecarAssets.Slot(n.toString(), p, SidecarAssets.State.UNUSED, label));
            }
        }
        return out;
    }

    // A slot whose file lives at a resource location (texture_path), not next to the json: try each
    // accepted name in that location's directory; when none is in this pack, report the canonical
    // name EXTERNAL (it resolves from vanilla or another pack, still previewable off the live stack).
    private static SidecarAssets.Slot remoteSlot(TextureSlot slot, PackWorkspace pack, Side side) {
        ResourceLocation location = slot.remoteLocation();
        String base = side == Side.SERVER_DATA ? "data" : "assets";
        String path = location.getPath();
        int slash = path.lastIndexOf('/');
        String dir = slash < 0 ? "" : path.substring(0, slash + 1);

        SidecarAssets.Slot canonical = null;
        for (String fileName : slot.acceptedNames()) {
            SidecarAssets.Slot candidate = SidecarAssets.referenced(pack, base,
                    location.withPath(dir + fileName), "", "", slot.label());
            if (candidate.state() == SidecarAssets.State.PRESENT) return candidate;
            if (canonical == null) canonical = candidate;
        }
        return canonical;
    }
}
