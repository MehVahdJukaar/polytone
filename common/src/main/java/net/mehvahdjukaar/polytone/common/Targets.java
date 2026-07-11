package net.mehvahdjukaar.polytone.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.regex.Pattern;

import static net.mehvahdjukaar.polytone.common.struc.ListUtils.mergeList;

public record Targets(List<Entry> entries) {

    public static final Targets EMPTY = new Targets(List.of());

    public static Targets ofIds(Identifier... blocks) {
        List<Entry> entries = new ArrayList<>();
        for (Identifier id : blocks) {
            entries.add(new SimpleLocation(id));
        }
        return new Targets(entries);
    }

    public static Targets ofIds(Set<Identifier> blocks) {
        List<Entry> entries = new ArrayList<>();
        for (Identifier id : blocks) {
            entries.add(new SimpleLocation(id));
        }
        return new Targets(entries);
    }

    public static Targets ofOptionalIds(Set<Identifier> blocks) {
        List<Entry> entries = new ArrayList<>();
        for (Identifier id : blocks) {
            entries.add(new OptionalEntry(new SimpleLocation(id), false));
        }
        return new Targets(entries);
    }

    public <T> Collection<Holder<T>> compute(Identifier fileId, HolderLookup.RegistryLookup<T> registry) {

        Set<Holder<T>> set = new HashSet<>();
        ResourceKey<T> key = regKey(fileId, registry);
        Optional<Holder.Reference<T>> implicitTarget = registry.get(key);
        if (!entries.isEmpty()) {
            if (implicitTarget.isPresent()) {
                Polytone.LOGGER.warn("Found Polytone file with explicit Targets ({}) also having a valid IMPLICIT (file path) Target ({})." +
                        "Consider moving it under your OWN namespace to avoid overriding other packs modifiers with the same path", entries, fileId);
            }
            for (var entry : entries) {
                try {
                    for (var holder : entry.get(registry)) {
                        set.add(holder);
                    }
                } catch (Exception e) {
                    if (!Polytone.CONFIGS.isLenientLoading()){
                        throw new IllegalStateException("Failed to parse some target(s) for polytone file " + fileId, e);
                    }
                }
            }
        }
        //no explicit targets. use its own ID instead
        else {
            if (implicitTarget.isPresent()) {
                set.add(implicitTarget.get());
            } else {
                Polytone.LOGGER.error("Found Polytone file {} with no valid implicit targets and no explicit targets from registry {}",
                        fileId, registry);
            }
        }
        return set;
    }

    private static <T> @NonNull ResourceKey<T> regKey(Identifier fileId, HolderLookup.RegistryLookup<T> registry) {
        return ResourceKey.create((ResourceKey<? extends Registry<T>>) registry.key(), fileId);
    }

    public Targets merge(Targets other) {
        return new Targets(mergeList(entries, other.entries));
    }

    public void addSimple(@NotNull Identifier id) {
        Entry simpleLocation = new SimpleLocation(id);
        this.entries.add(simpleLocation);
    }

    public void addTag(Identifier id) {
        Entry tagLocation = new TagLocation(id);
        this.entries.add(tagLocation);
    }

    private interface Entry {
        <T> Iterable<? extends Holder<T>> get(HolderLookup.RegistryLookup<T> reg);
    }

    private static final Codec<Entry> SIMPLE_TAG_OR_REGEX_ENTRY_CODEC = SchemaCodecs.alternatives(
            "id", SimpleLocation.SIMPLE_CODEC,
            "tag", TagLocation.TAG_CODEC,
            "regex", RegexLocation.REGEX_CODEC);

    private static final Codec<Entry> ENTRY_CODEC = Codec.withAlternative(SIMPLE_TAG_OR_REGEX_ENTRY_CODEC, OptionalEntry.OPTIONAL_CODEC);

    public static final Codec<Targets> CODEC = SchemaCodecs.singleOrList(ENTRY_CODEC)
            .xmap(Targets::new, t -> t.entries);

    private record OptionalEntry(Entry entry, boolean required) implements Entry {
        public static final SchemaCodec<OptionalEntry> OPTIONAL_CODEC = SchemaRecord.create(OptionalEntry.class, i -> i.group(
                i.field("id", SIMPLE_TAG_OR_REGEX_ENTRY_CODEC, OptionalEntry::entry),
                i.optional("required", Codec.BOOL, true, OptionalEntry::required)
        ).apply(i, OptionalEntry::new));

        @Override
        public <T> Iterable<? extends Holder<T>> get(HolderLookup.RegistryLookup<T> reg) {
            try {
                return entry.get(reg);
            } catch (IllegalStateException e) {
                if (required) throw e;
                return List.of();
            }
        }

        @Override
        public @NonNull String toString() {
            return "OPT{" +
                    "entry=" + entry +
                    ", required=" + required +
                    '}';
        }
    }

    private record SimpleLocation(@NotNull Identifier id) implements Entry {
        public static final Codec<SimpleLocation> SIMPLE_CODEC = Identifier.CODEC
                .xmap(SimpleLocation::new, s -> s.id);

        @Override
        public <T> Iterable<? extends Holder<T>> get(HolderLookup.RegistryLookup<T> reg) {
            var holder = reg.get(regKey(id, reg));
            if (holder.isEmpty() && Polytone.isFutureId(id)) {
                return List.of();
            }
            return List.of(holder.orElseThrow(() -> new IllegalStateException("Entry not found in registry " + reg.key() + ": " + id)));
        }

    }

    private record TagLocation(Identifier id) implements Entry {
        public static final Codec<TagLocation> TAG_CODEC = com.mojang.serialization.Codec.STRING.flatXmap(s -> {
                    if (s.startsWith("#")) {
                        return Identifier.read(s.substring(1)).map(TagLocation::new);
                    } else return DataResult.error(() -> "Tag location must start with #");
                },
                id -> DataResult.success(id.toString()));


        @Override
        public <T> Iterable<Holder<T>> get(HolderLookup.RegistryLookup<T> reg) {
            TagKey<T> key = TagKey.create((ResourceKey<? extends Registry<T>>) reg.key(), id);
            return PlatStuff.getTagEntries(reg, key);
        }

        @Override
        public @NonNull String toString() {
            return "#" + id;
        }
    }

    private record RegexLocation(Pattern regex) implements Entry {
        public static final Codec<RegexLocation> REGEX_CODEC = Codec.STRING.xmap(
                s -> new RegexLocation(Pattern.compile(s)),
                r -> r.regex.pattern()
        );

        @Override
        public <T> Iterable<? extends Holder<T>> get(HolderLookup.RegistryLookup<T> reg) {
            return reg.listElements().filter(e -> regex.matcher(e.key().identifier().toString())
                    .matches()).toList();
        }

        @Override
        public @NonNull String toString() {
            return "RE: " + regex.pattern();
        }
    }
}
