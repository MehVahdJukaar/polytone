package net.mehvahdjukaar.polytone.common;

import com.google.common.base.Joiner;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// every layer's files read once, so a lookup is a map hit instead of a disk call per layer
public class OverlayIndex {

    private static final Joiner PATH_JOINER = Joiner.on("/");

    private final Map<PackType, Map<String, NavigableMap<String, IoSupplier<InputStream>>>> files = new EnumMap<>(PackType.class);
    private final Set<String> invalidNamespaces = new HashSet<>();
    private boolean useVanilla = false;

    private OverlayIndex() {
        for (PackType type : PackType.values()) files.put(type, new HashMap<>());
    }

    // null keeps vanilla's lookups, for layers that aren't plain folders or zips
    public static @Nullable OverlayIndex of(List<PackResources> stack) {
        for (PackResources layer : stack) {
            if (layer.getClass() != PathPackResources.class && layer.getClass() != FilePackResources.class) return null;
        }
        try {
            OverlayIndex index = new OverlayIndex();
            Map<ZipFile, List<? extends ZipEntry>> zipEntries = new IdentityHashMap<>();
            // the stack is top first, so go bottom up and let higher layers overwrite
            for (PackResources layer : stack.reversed()) {
                for (PackType type : PackType.values()) {
                    if (layer instanceof PathPackResources folder) {
                        index.addFolder(type, folder.root.resolve(type.getDirectory()));
                    } else if (layer instanceof FilePackResources zip) {
                        index.addZip(type, zip, zipEntries);
                    }
                }
            }
            for (String namespace : index.invalidNamespaces) {
                Polytone.LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", namespace, stack.getFirst().location().id());
            }
            return index.useVanilla ? null : index;
        } catch (Exception e) {
            Polytone.LOGGER.error("Failed to index pack {}", stack.getFirst().location().id(), e);
            return null;
        }
    }

    private void addFolder(PackType type, Path typeDir) {
        Map<String, NavigableMap<String, IoSupplier<InputStream>>> namespaces = this.files.get(type);
        try {
            Files.walkFileTree(typeDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.getNameCount() != typeDir.getNameCount() + 1) return FileVisitResult.CONTINUE;
                    String namespace = dir.getFileName().toString();
                    if (!Identifier.isValidNamespace(namespace)) {
                        invalidNamespaces.add(namespace);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    namespaces.computeIfAbsent(namespace, n -> new TreeMap<>());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    // vanilla looks up links and cloud placeholders but doesn't list them, only its own disk calls do both
                    if (!attrs.isRegularFile()) {
                        useVanilla = true;
                        return FileVisitResult.TERMINATE;
                    }
                    Path relative = typeDir.relativize(file);
                    if (relative.getNameCount() > 1) {
                        namespaces.get(relative.getName(0).toString())
                                .put(PATH_JOINER.join(relative.subpath(1, relative.getNameCount())), IoSupplier.create(file));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (NoSuchFileException | NotDirectoryException ignored) {
        } catch (IOException e) {
            Polytone.LOGGER.error("Failed to index pack directory {}", typeDir, e);
            this.useVanilla = true;
        }
    }

    private void addZip(PackType type, FilePackResources pack, Map<ZipFile, List<? extends ZipEntry>> zipEntries) {
        ZipFile zipFile = pack.zipFileAccess.getOrCreateZipFile();
        if (zipFile == null) return;
        Map<String, NavigableMap<String, IoSupplier<InputStream>>> namespaces = this.files.get(type);
        String root = (pack.prefix.isEmpty() ? "" : pack.prefix + "/") + type.getDirectory() + "/";
        // overlays share the base pack's zip, so its entries are read once
        for (ZipEntry entry : zipEntries.computeIfAbsent(zipFile, z -> z.stream().toList())) {
            String name = entry.getName();
            if (!name.startsWith(root)) continue;
            int slash = name.indexOf('/', root.length());
            String namespace = slash == -1 ? name.substring(root.length()) : name.substring(root.length(), slash);
            if (namespace.isEmpty()) continue;
            if (!Identifier.isValidNamespace(namespace)) {
                invalidNamespaces.add(namespace);
                continue;
            }
            NavigableMap<String, IoSupplier<InputStream>> files = namespaces.computeIfAbsent(namespace, n -> new TreeMap<>());
            if (slash != -1 && !entry.isDirectory()) files.put(name.substring(slash + 1), IoSupplier.create(zipFile, entry));
        }
    }

    public @Nullable IoSupplier<InputStream> getResource(PackType type, Identifier location) {
        NavigableMap<String, IoSupplier<InputStream>> namespace = this.files.get(type).get(location.getNamespace());
        return namespace == null ? null : namespace.get(location.getPath());
    }

    public void listResources(PackType type, String namespace, String directory, PackResources.ResourceOutput output) {
        NavigableMap<String, IoSupplier<InputStream>> files = this.files.get(type).get(namespace);
        if (files == null) return;
        String prefix = directory + "/";
        files.subMap(prefix, prefix + Character.MAX_VALUE).forEach((path, resource) -> {
            Identifier id = Identifier.tryBuild(namespace, path);
            if (id != null) output.accept(id, resource);
            else Util.logAndPauseIfInIde(String.format(Locale.ROOT, "Invalid path in pack: %s:%s, ignoring", namespace, path));
        });
    }

    public Set<String> getNamespaces(PackType type) {
        return new HashSet<>(this.files.get(type).keySet());
    }
}
