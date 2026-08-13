package net.mehvahdjukaar.polytone.compat.nautilus;

import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.nautilus.workbench.FileNamesUtil;
import net.mehvahdjukaar.nautilus.workbench.PackWorkspace;
import net.mehvahdjukaar.nautilus.workbench.SidecarAssets;
import net.mehvahdjukaar.polytone.companion.ContentTextures;
import net.mehvahdjukaar.polytone.companion.TextureSlot;
import net.mehvahdjukaar.polytone.utils.StrUtils;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Bridges a content type's ContentTextures to the editor's SidecarAssets view, so the Nautilus tab
// shows a colormap's .png files: present, expected-but-missing and stray. Slot matching goes through
// the same TextureSlot.findFirstMatch rule as the reload driver, so the two can't drift.
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
        String stem = FileNamesUtil.stem(nameP.toString());

        Map<String, Path> byName = pack.childrenByName(dir); // regular files, lower-cased keys

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

        // stray files the naming convention associates with this stem but no slot consumed
        for (Path p : byName.values()) {
            if (claimed.contains(p)) continue;
            String name = p.getFileName().toString();
            String label = association.roleLabel(name, stem);
            if (label != null) {
                out.add(new SidecarAssets.Slot(name, p, SidecarAssets.State.UNUSED, label));
            }
        }
        return out;
    }

    // A slot whose file lives at a texture_path location instead of next to the json. When no accepted
    // name is in this pack, report the canonical one EXTERNAL: it still resolves from vanilla or another pack.
    private static SidecarAssets.Slot remoteSlot(TextureSlot slot, PackWorkspace pack, Side side) {
        ResourceLocation location = slot.remoteLocation();
        String dir = StrUtils.directoryOf(location.getPath());
        List<ResourceLocation> candidates = slot.acceptedNames().stream()
                .map(name -> location.withPath(dir + name)).toList();
        return SidecarAssets.referencedFirstPresent(pack, side, candidates, "", "", slot.label());
    }
}
