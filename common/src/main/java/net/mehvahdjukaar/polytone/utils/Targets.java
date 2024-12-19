package net.mehvahdjukaar.polytone.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.*;
import java.util.regex.Pattern;

import static net.mehvahdjukaar.polytone.utils.ListUtils.mergeList;

public record Targets(List<Entry> entries) {

    public static final Targets EMPTY = new Targets(List.of());

    public static Targets ofIds(Set<ResourceLocation> blocks) {
        List<Entry> entries = new ArrayList<>();
        for (ResourceLocation id : blocks) {
            entries.add(new SimpleLocation(id));
        }
        return new Targets(entries);
    }

    public <T> Collection<Holder<T>> compute(ResourceLocation fileId, HolderLookup.RegistryLookup<T> registry) {

        Set<Holder<T>> set = new HashSet<>();
        ResourceKey regKey = registry.key();
        ResourceKey<T> key = ResourceKey.create(regKey, fileId);
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
                    throw new IllegalStateException("Failed to parse some target(s) for polytone file " + fileId, e);
                }
            }
        }
        //no explicit targets. use its own ID instead
        else {
            if (implicitTarget.isPresent()) {
                set.add(implicitTarget.get());
            } else {
                Polytone.LOGGER.error("Found Polytone file {} with no valid implicit targets and no explicit targets from registry {}", fileId, registry);
            }
        }
        return set;
    }

    public Targets merge(Targets other) {
        return new Targets(mergeList(entries, other.entries));
    }

    public void addSimple(ResourceLocation id) {
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

    private static final Codec<Entry> SIMPLE_TAG_OR_REGEX_ENTRY_CODEC = Codec.withAlternative(
            (Codec<Entry>) (Object) SimpleLocation.SIMPLE_CODEC,
            Codec.withAlternative((Codec<Entry>) (Object) TagLocation.TAG_CODEC, RegexLocation.REGEX_CODEC));

    private static final Codec<Entry> ENTRY_CODEC = Codec.withAlternative(SIMPLE_TAG_OR_REGEX_ENTRY_CODEC, OptionalEntry.OPTIONAL_CODEC);

    public static final Codec<Targets> CODEC = Codec.withAlternative(ENTRY_CODEC.xmap(List::of, List::getFirst),
            ENTRY_CODEC.listOf()).xmap(Targets::new, t -> t.entries);

    private record OptionalEntry(Entry entry, boolean required) implements Entry {
        public static final Codec<OptionalEntry> OPTIONAL_CODEC = RecordCodecBuilder.create(i -> i.group(
                SIMPLE_TAG_OR_REGEX_ENTRY_CODEC.fieldOf("id").forGetter(OptionalEntry::entry),
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("required", true).forGetter(OptionalEntry::required)
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
        public String toString() {
            return "OPT{" +
                    "entry=" + entry +
                    ", required=" + required +
                    '}';
        }
    }

    private record SimpleLocation(ResourceLocation id) implements Entry {
        public static final Codec<SimpleLocation> SIMPLE_CODEC = ResourceLocation.CODEC
                .xmap(SimpleLocation::new, s -> s.id);

        @Override
        public <T> Iterable<? extends Holder<T>> get(HolderLookup.RegistryLookup<T> reg) {
            ResourceKey k = reg.key();
            ResourceKey<T> key = ResourceKey.create(k, this.id);
            return List.of(reg.getOrThrow(key));
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
            ResourceKey k = reg.key();
            TagKey<T> key = TagKey.create(k, id);
            return reg.getOrThrow(key);
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
