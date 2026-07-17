package net.mehvahdjukaar.polytone.compat.nautilus;

import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.nautilus.workbench.PackWorkspace;
import net.mehvahdjukaar.nautilus.workbench.SidecarAssets;
import net.mehvahdjukaar.polytone.companion.CompanionSlot;
import net.mehvahdjukaar.polytone.companion.CompanionSpec;
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

// Bridges a content type's CompanionSpec (the runtime-side companion-file contract) to the editor's
// SidecarAssets view, so the Nautilus tab shows a colormap's .png textures next to its json: present,
// expected-but-missing, and stray-but-unused. Same naming primitives as the reload driver
// (ColormapTextures.fill), so the two can't drift.
final class CompanionSidecars {

    static SidecarAssets of(CompanionSpec<?> spec, Side side) {
        return (jsonFile, pack, parsedValue) -> {
            try {
                return discover(spec, side, jsonFile, pack, parsedValue);
            } catch (Exception e) {
                // discover() must never throw on IO/parse problems - an empty view is the safe fallback
                return List.of();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static List<SidecarAssets.Slot> discover(CompanionSpec<?> rawSpec, Side side,
                                                     Path jsonFile, PackWorkspace pack, Object parsedValue) {
        CompanionSpec<Object> spec = (CompanionSpec<Object>) rawSpec;

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

        List<CompanionSlot> slots;
        try {
            slots = spec.expectedSlots(parsedValue, stem);
        } catch (Exception e) {
            slots = List.of();
        }

        for (CompanionSlot slot : slots) {
            if (slot.remoteLocation() != null) {
                out.add(remoteSlot(slot, pack, side));
                continue;
            }
            Path found = null;
            for (String accepted : slot.acceptedNames()) {
                Path p = byName.get(accepted.toLowerCase(Locale.ROOT));
                if (p != null) {
                    found = p;
                    break;
                }
            }
            if (found != null) {
                // a file matched by two slots is emitted once, not as two PRESENT rows
                if (claimed.add(found)) {
                    out.add(new SidecarAssets.Slot(found.getFileName().toString(), found,
                            SidecarAssets.State.PRESENT, slot.label()));
                }
            } else if (slot.required()) {
                // optional-and-absent slots are fine to omit; only flag required ones as missing
                out.add(new SidecarAssets.Slot(slot.canonicalName(), null,
                        SidecarAssets.State.MISSING, slot.label()));
            }
        }

        // stray siblings the naming convention associates with this stem but no slot consumed
        for (Path p : siblings) {
            if (claimed.contains(p)) continue;
            Path n = p.getFileName();
            if (n == null || !Files.isRegularFile(p)) continue;
            String label = safeClassify(spec, n.toString(), stem);
            if (label != null) {
                out.add(new SidecarAssets.Slot(n.toString(), p, SidecarAssets.State.UNUSED, label));
            }
        }
        return out;
    }

    // A slot whose file lives at a resource location (texture_path), not next to the json.
    private static SidecarAssets.Slot remoteSlot(CompanionSlot slot, PackWorkspace pack, Side side) {
        ResourceLocation loc = ResourceLocation.parse(slot.remoteLocation());
        String base = side == Side.SERVER_DATA ? "data" : "assets";
        String path = loc.getPath();
        int slash = path.lastIndexOf('/');
        String subDir = slash < 0 ? "" : path.substring(0, slash + 1);

        for (String accepted : slot.acceptedNames()) {
            Path candidate = pack.root().resolve(base).resolve(loc.getNamespace()).resolve(subDir + accepted);
            if (Files.isRegularFile(candidate)) {
                return new SidecarAssets.Slot(accepted, candidate, SidecarAssets.State.PRESENT, slot.label());
            }
        }
        // not in this pack - it resolves from vanilla or another pack, nothing local to edit
        return new SidecarAssets.Slot(slot.canonicalName(), null,
                SidecarAssets.State.EXTERNAL, slot.label(), loc);
    }

    private static String safeClassify(CompanionSpec<?> spec, String fileName, String stem) {
        try {
            return spec.classify(fileName, stem);
        } catch (Exception e) {
            return null;
        }
    }
}
