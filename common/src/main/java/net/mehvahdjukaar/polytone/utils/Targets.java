package net.mehvahdjukaar.polytone.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

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

    public <T> Collection<Holder<T>> compute(ResourceLocation fileId, Registry<T> registry) {

        Set<Holder<T>> set = new HashSet<>();
        ResourceKey<T> key = ResourceKey.create(registry.key(), fileId);
        Optional<Holder.Reference<T>> implicitTarget = registry.getHolder(key);
        if (!entries.isEmpty()) {
            if (implicitTarget.isPresent()) {
                Polytone.LOGGER.warn("Found Polytone file with explicit Targets ({}) also having a valid IMPLICIT (file path) Target ({})." +
                        "Consider moving it under your OWN namespace to avoid overriding other packs modifiers with the same path", entries, fileId);
            }
            for (var entry : entries) {
                for (var holder : entry.get(registry)) {
                    set.add(holder);
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
        <T> Iterable<? extends Holder<T>> get(Registry<T> reg);
    }

    private static final Codec<Entry> SIMPLE_TAG_OR_REGEX_ENTRY_CODEC = CodecUtil.withAlternative(
            (Codec<Entry>) (Object) SimpleLocation.SIMPLE_CODEC,
            CodecUtil.withAlternative((Codec<Entry>) (Object) TagLocation.TAG_CODEC, RegexLocation.REGEX_CODEC));

    private static final Codec<Entry> ENTRY_CODEC = CodecUtil.withAlternative(SIMPLE_TAG_OR_REGEX_ENTRY_CODEC, OptionalEntry.OPTIONAL_CODEC);

    public static final Codec<Targets> CODEC = CodecUtil.withAlternative(ENTRY_CODEC.xmap(List::of, l->l.get(0)),
            ENTRY_CODEC.listOf()).xmap(Targets::new, t -> t.entries);

    private record OptionalEntry(Entry entry, boolean required) implements Entry {
        public static final Codec<OptionalEntry> OPTIONAL_CODEC = RecordCodecBuilder.create(i -> i.group(
                SIMPLE_TAG_OR_REGEX_ENTRY_CODEC.fieldOf("id").forGetter(OptionalEntry::entry),
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("required", true).forGetter(OptionalEntry::required)
        ).apply(i, OptionalEntry::new));

        @Override
        public <T> Iterable<? extends Holder<T>> get(Registry<T> reg) {
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
        public <T> Iterable<? extends Holder<T>> get(Registry<T> reg) {
            ResourceKey<T> key = ResourceKey.create(reg.key(), id);
            return List.of(reg.getHolder(key).orElseThrow(() -> new IllegalStateException("Entry not found: " + id)));
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
        public <T> Iterable<Holder<T>> get(Registry<T> reg) {
            TagKey<T> key = TagKey.create(reg.key(), id);
            return reg.getTag(key).orElseThrow(() -> new IllegalStateException("Tag not found: " + id));
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
        public <T> Iterable<? extends Holder<T>> get(Registry<T> reg) {
            return reg.holders().filter(e -> regex.matcher(e.key().location().toString())
                    .matches()).toList();
        }

        @Override
        public String toString() {
            return "RE: " + regex.pattern();
        }
    }
}
