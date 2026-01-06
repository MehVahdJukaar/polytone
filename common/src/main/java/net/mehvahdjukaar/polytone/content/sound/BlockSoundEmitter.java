package net.mehvahdjukaar.polytone.content.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.block.BlockClientTickable;
import net.mehvahdjukaar.polytone.common.exp.impl.BlockContextExpression;
import net.mehvahdjukaar.polytone.content.block.TickSource;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Optional;

public record BlockSoundEmitter(
        SoundEvent sound,
        SoundSource category,
        IBlockExp chance,
        IBlockExp x,
        IBlockExp y,
        IBlockExp z,
        IBlockExp volume,
        IBlockExp pitch,
        boolean distanceDelay,
        RuleTest predicate,
        @Deprecated(forRemoval = true) TickSource spawnSource,
        Optional<HolderSet<Biome>> biomes) implements BlockClientTickable {

    private static final Codec<SoundSource> SOUND_SOURCE_CODEC =
            Codec.STRING.comapFlatMap(s -> DataResult.success(SoundSource.valueOf(s.toLowerCase(Locale.ROOT))),
                    s -> s.getName().toLowerCase(Locale.ROOT));

    public static final Codec<BlockSoundEmitter> CODEC = RecordCodecBuilder.create(i -> i.group(
            CodecUtils.forwardAwareSoundEvent().fieldOf("sound").forGetter(BlockSoundEmitter::sound),
            SOUND_SOURCE_CODEC.optionalFieldOf("source", SoundSource.BLOCKS).forGetter(BlockSoundEmitter::category),
            IBlockExp.CODEC.optionalFieldOf("chance", IBlockExp.ONE).forGetter(BlockSoundEmitter::chance),
            IBlockExp.CODEC.optionalFieldOf("x", IBlockExp.ZERO).forGetter(BlockSoundEmitter::x),
            IBlockExp.CODEC.optionalFieldOf("y", IBlockExp.ZERO).forGetter(BlockSoundEmitter::y),
            IBlockExp.CODEC.optionalFieldOf("z", IBlockExp.ZERO).forGetter(BlockSoundEmitter::z),
            IBlockExp.CODEC.optionalFieldOf("volume", IBlockExp.ZERO).forGetter(BlockSoundEmitter::volume),
            IBlockExp.CODEC.optionalFieldOf("pitch", IBlockExp.ZERO).forGetter(BlockSoundEmitter::pitch),
            Codec.BOOL.optionalFieldOf("distance_delay", false).forGetter(BlockSoundEmitter::distanceDelay),
            CodecUtils.lenientWithLog(RuleTest.CODEC, "state_predicate", AlwaysTrueTest.INSTANCE).forGetter(BlockSoundEmitter::predicate),
            TickSource.CODEC.optionalFieldOf("spawn_source", TickSource.ANIMATE_TICK).forGetter(BlockSoundEmitter::spawnSource),
            CodecUtils.forwardAwareHomogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(BlockSoundEmitter::biomes)
    ).apply(i, BlockSoundEmitter::new));


    @Override
    public void tick(Level level, BlockPos pos, BlockState state, TickSource source) {
        if (source != spawnSource) return;
        double spawnChance = chance.evaluate(level, pos, state);
        if (level.random.nextFloat() < spawnChance) {
            if (biomes.isPresent()) {
                var biome = level.getBiome(pos);
                if (!biomes.get().contains(biome)) return;
            }

            Vec3 vec = pos.getCenter().add(
                    x.evaluate(level, pos, state),
                    y.evaluate(level, pos, state),
                    z.evaluate(level, pos, state));

            float v = (float) volume.evaluate(level, pos, state);
            float p = (float) pitch.evaluate(level, pos, state);

            level.playLocalSound(vec.x, vec.y, vec.z,
                    sound, category, v, p, false);
        }
    }


}
