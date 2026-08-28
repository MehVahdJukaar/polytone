package net.mehvahdjukaar.polytone.content.light;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IParticleExp;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.function.Function;

public sealed interface ColoredLightEntry {

    // dispatch needs the key to be there, so a file without target_type is read as a block entry
    Codec<ColoredLightEntry> CODEC = Codec.withAlternative(
            Type.CODEC.dispatch("target_type", ColoredLightEntry::type, Type::codec),
            Blocks.CODEC.codec());

    Type type();

    Targets targets();

    record Blocks(Targets targets, ColoredLight<IBlockExp> light, RuleTest predicate) implements ColoredLightEntry {
        static final MapCodec<Blocks> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Targets.CODEC.fieldOf("targets").forGetter(Blocks::targets),
                ColoredLight.codec(IBlockExp.MVEL_CODEC, IBlockExp::constant).fieldOf("colored_light").forGetter(Blocks::light),
                SchemaCodecs.lenientWithLog(RuleTest.CODEC, "state_predicate", AlwaysTrueTest.INSTANCE).forGetter(Blocks::predicate)
        ).apply(i, Blocks::new));

        @Override
        public Type type() {
            return Type.BLOCK;
        }
    }

    record Entities(Targets targets, ColoredLight<IEntityExp> light) implements ColoredLightEntry {
        static final MapCodec<Entities> CODEC = ofEntity(Entities::new, Entities::targets, Entities::light);

        @Override
        public Type type() {
            return Type.ENTITY;
        }
    }

    record Items(Targets targets, ColoredLight<IEntityExp> light) implements ColoredLightEntry {
        static final MapCodec<Items> CODEC = ofEntity(Items::new, Items::targets, Items::light);

        @Override
        public Type type() {
            return Type.ITEM;
        }
    }

    record Particles(Targets targets, ColoredLight<IParticleExp> light) implements ColoredLightEntry {
        static final MapCodec<Particles> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Targets.CODEC.fieldOf("targets").forGetter(Particles::targets),
                ColoredLight.codec(IParticleExp.CODEC, c -> (p, l) -> c).fieldOf("colored_light").forGetter(Particles::light)
        ).apply(i, Particles::new));

        @Override
        public Type type() {
            return Type.PARTICLE;
        }
    }

    private static <T extends ColoredLightEntry> MapCodec<T> ofEntity(
            java.util.function.BiFunction<Targets, ColoredLight<IEntityExp>, T> factory,
            Function<T, Targets> targets, Function<T, ColoredLight<IEntityExp>> light) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Targets.CODEC.fieldOf("targets").forGetter(targets::apply),
                ColoredLight.codec(IEntityExp.CODEC, c -> e -> c).fieldOf("colored_light").forGetter(light::apply)
        ).apply(i, factory::apply));
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

        MapCodec<? extends ColoredLightEntry> codec() {
            return codec;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
