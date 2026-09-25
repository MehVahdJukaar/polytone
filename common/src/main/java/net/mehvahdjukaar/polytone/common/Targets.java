package net.mehvahdjukaar.polytone.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

import static net.mehvahdjukaar.polytone.common.Utils.mergeList;

public record Targets(List<Entry> entries) {

    public static final Targets EMPTY = new Targets(List.of());

    public static Targets ofIds(ResourceLocation... blocks) {
        List<Entry> entries = new ArrayList<>();
        for (ResourceLocation id : blocks) {
            entries.add(new SimpleLocation(id));
        }
        return new Targets(entries);
    }

    public static Targets ofIds(Set<ResourceLocation> blocks) {
        List<Entry> entries = new ArrayList<>();
        for (ResourceLocation id : blocks) {
            entries.add(new SimpleLocation(id));
        }
        return new Targets(entries);
    }

    public static Targets ofOptionalIds(Set<ResourceLocation> blocks) {
        List<Entry> entries = new ArrayList<>();
        for (ResourceLocation id : blocks) {
            entries.add(new OptionalEntry(new SimpleLocation(id), false));
        }
        return new Targets(entries);
    }

    //mainly for legacy parsing
    public static Targets legacyIds(Collection<String> names) {
        List<Entry> entries = new ArrayList<>();
        for (String name : names) {
            boolean isTag = name.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(isTag ? name.substring(1) : name);
            if (id == null) {
                Polytone.LOGGER.warn("Skipping invalid block name in legacy block list: {}", name);
                continue;
            }
            Entry entry = isTag ? new TagLocation(id) : new SimpleLocation(id);
            entries.add(new OptionalEntry(entry, false));
        }
        return new Targets(entries);
    }

    public <T> Collection<Holder<T>> compute(ResourceLocation fileId, HolderLookup.RegistryLookup<T> registry) {

        Set<Holder<T>> set = new HashSet<>();
        ResourceKey<T> registryKey = ResourceKey.create((ResourceKey) registry.key(), fileId);
        Optional<Holder.Reference<T>> implicitTarget = registry.get(registryKey);
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
                } catch (MissingEntryException e) {
                    if (!e.id.getNamespace().equals("minecraft")){
                        throw e;
                    }
                    Polytone.LOGGER.error("Found missing ID in minecraft namespace: {}. Polytone will skip it but this remains a bug of the Resource Pack. Optional entries or resource conditions should be used to maintain backward compatibility instead.", e.id);
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

    public Targets merge(Targets other) {
        return new Targets(mergeList(entries, other.entries));
    }

    public void addSimple(@NotNull ResourceLocation id) {
        Entry simpleLocation = new SimpleLocation(id);
        this.entries.add(simpleLocation);
    }

    public void addTag(ResourceLocation id) {
        Entry tagLocation = new TagLocation(id);
        this.entries.add(tagLocation);
    }

    private interface Entry {
        <T> Iterable<? extends Holder<T>> get(HolderLookup.RegistryLookup<T> reg);
    }

    // tag first cuz SOME mod is making resource locations accept # symbols...
    private static final Codec<Entry> SIMPLE_TAG_OR_REGEX_ENTRY_CODEC = SchemaCodecs.labeled(
            Codec.withAlternative(
                    (Codec<Entry>) (Object) TagLocation.TAG_CODEC,
                    Codec.withAlternative((Codec<Entry>) (Object) SimpleLocation.SIMPLE_CODEC, RegexLocation.REGEX_CODEC)),
            SchemaCodecs.alt("tag", TagLocation.TAG_CODEC),
            SchemaCodecs.alt("id", SimpleLocation.SIMPLE_CODEC),
            SchemaCodecs.alt("regex", RegexLocation.REGEX_CODEC));

    private static final Codec<Entry> ENTRY_CODEC = SchemaCodecs.labeled(
            Codec.withAlternative(SIMPLE_TAG_OR_REGEX_ENTRY_CODEC, OptionalEntry.OPTIONAL_CODEC),
            SchemaCodecs.alt("entry", SIMPLE_TAG_OR_REGEX_ENTRY_CODEC),
            SchemaCodecs.alt("optional id", OptionalEntry.OPTIONAL_CODEC));

    // Labeled AnyOf alternatives splice flat, so the selector shows [id, tag, regex, optional id, list].
    public static final Codec<Targets> CODEC = SchemaCodecs.labeled(
            Codec.withAlternative(ENTRY_CODEC.xmap(List::of, List::getFirst), ENTRY_CODEC.listOf())
                    .xmap(Targets::new, t -> t.entries),
            SchemaCodecs.alt("single", ENTRY_CODEC),
            SchemaCodecs.alt("list", ENTRY_CODEC.listOf()));

    private record OptionalEntry(Entry entry, boolean required) implements Entry {
        public static final Codec<OptionalEntry> OPTIONAL_CODEC = RecordCodecBuilder.create(i -> i.group(
                SIMPLE_TAG_OR_REGEX_ENTRY_CODEC.fieldOf("id").forGetter(OptionalEntry::entry),
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("required", true).forGetter(OptionalEntry::required)
        ).apply(i, OptionalEntry::new));

        @Override
        public <T> Iterable<? extends Holder<T>> get(HolderLookup.RegistryLookup<T> reg) {
            try {
                return entry.get(reg);
            } catch (MissingEntryException e) {
                if (required) throw e;
                return List.of();
            }
        }

        @Override
        public String toString() {
            return "OPT{" +
                    "entry=" + entry +
                    ", required=" + required +
                    '}';
        }
    }

    private record SimpleLocation(@NotNull ResourceLocation id) implements Entry {
        public static final Codec<SimpleLocation> SIMPLE_CODEC = ResourceLocation.CODEC
                .xmap(SimpleLocation::new, s -> s.id);

        @Override
        public <T> Iterable<? extends Holder<T>> get(HolderLookup.RegistryLookup<T> reg) {
            ResourceKey<T> key = ResourceKey.create((ResourceKey) reg.key(), id);
            var holder = reg.get(key);
            if (holder.isEmpty()) throw new MissingEntryException(id);
            return List.of(holder.get());
        }

    }

    private static class MissingEntryException extends IllegalStateException {
        final ResourceLocation id;

        private MissingEntryException(ResourceLocation id) {
            super("Entry not found: " + id);
            this.id = id;
        }
    }

    private record TagLocation(ResourceLocation id) implements Entry {
        public static final Codec<TagLocation> TAG_CODEC = com.mojang.serialization.Codec.STRING.flatXmap(s -> {
                    if (s.startsWith("#")) {
                        return ResourceLocation.read(s.substring(1)).map(TagLocation::new);
                    } else return DataResult.error(() -> "Tag location must start with #");
                },
                id -> DataResult.success(id.toString()));


        @Override
        public <T> Iterable<Holder<T>> get(HolderLookup.RegistryLookup<T> reg) {
            TagKey<T> key = TagKey.create((ResourceKey) reg.key(), id);
            return PlatStuff.getTagEntries(reg, key);
        }

        @Override
        public String toString() {
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
            return reg.listElements().filter(e -> regex.matcher(e.key().location().toString())
                    .matches()).toList();
        }

        @Override
        public String toString() {
            return "RE: " + regex.pattern();
        }
    }
}
