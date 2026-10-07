package net.mehvahdjukaar.polytone.content.shaders.light;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.Targets;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.function.Function;

public sealed interface ColoredLightEntry {

    //dispath needs the key to be there, so a file without target_type is read as a block entry
    Codec<ColoredLightEntry> CODEC = Codec.withAlternative(
            Type.CODEC.dispatch("target_type", ColoredLightEntry::type, Type::codec),
            Blocks.CODEC.codec());

    Type type();

    Targets targets();

    record Blocks(Targets targets, ColoredLight.BlockLight light, RuleTest predicate) implements ColoredLightEntry {
        static final MapCodec<Blocks> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Targets.codec(Registries.BLOCK).fieldOf("targets").forGetter(Blocks::targets),
                ColoredLight.BlockLight.CODEC.fieldOf("colored_light").forGetter(Blocks::light),
                SchemaCodecs.lenientWithLog(RuleTest.CODEC, "state_predicate", AlwaysTrueTest.INSTANCE).forGetter(Blocks::predicate)
        ).apply(i, Blocks::new));

        @Override
        public Type type() {
            return Type.BLOCK;
        }
    }

    record Entities(Targets targets, ColoredLight.EntityLight light) implements ColoredLightEntry {
        static final MapCodec<Entities> CODEC = ofEntity(Registries.ENTITY_TYPE, Entities::new, Entities::targets, Entities::light);

        @Override
        public Type type() {
            return Type.ENTITY;
        }
    }

    record Items(Targets targets, ColoredLight.EntityLight light) implements ColoredLightEntry {
        static final MapCodec<Items> CODEC = ofEntity(Registries.ITEM, Items::new, Items::targets, Items::light);

        @Override
        public Type type() {
            return Type.ITEM;
        }
    }

    record Particles(Targets targets, ColoredLight.ParticleLight light) implements ColoredLightEntry {
        static final MapCodec<Particles> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Targets.codec(Registries.PARTICLE_TYPE).fieldOf("targets").forGetter(Particles::targets),
                ColoredLight.ParticleLight.CODEC.fieldOf("colored_light").forGetter(Particles::light)
        ).apply(i, Particles::new));

        @Override
        public Type type() {
            return Type.PARTICLE;
        }
    }

    private static <T extends ColoredLightEntry> MapCodec<T> ofEntity(
            ResourceKey<? extends Registry<?>> registry, java.util.function.BiFunction<Targets, ColoredLight.EntityLight, T> factory,
            Function<T, Targets> targets, Function<T, ColoredLight.EntityLight> light) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Targets.codec(registry).fieldOf("targets").forGetter(targets),
                ColoredLight.EntityLight.CODEC.fieldOf("colored_light").forGetter(light)
        ).apply(i, factory));
    }

    enum Type implements StringRepresentable {
        BLOCK("block", Blocks.CODEC),
        ENTITY("entity", Entities.CODEC),
        ITEM("item", Items.CODEC),
        PARTICLE("particle", Particles.CODEC);

        static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;
        private final MapCodec<? extends ColoredLightEntry> codec;

        Type(String name, MapCodec<? extends ColoredLightEntry> codec) {
            this.name = name;
            this.codec = codec;
        }

        public MapCodec<? extends ColoredLightEntry> codec() {
            return codec;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
