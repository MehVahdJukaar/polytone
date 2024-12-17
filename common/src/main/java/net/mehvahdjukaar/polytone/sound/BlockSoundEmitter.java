package net.mehvahdjukaar.polytone.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.block.BlockClientTickable;
import net.mehvahdjukaar.polytone.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.block.BlockSetTypeProvider;
import net.mehvahdjukaar.polytone.utils.LazyHolderSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Optional;

public record BlockSoundEmitter(
        SoundEvent sound,
        SoundSource category,
        BlockContextExpression chance,
        BlockContextExpression x,
        BlockContextExpression y,
        BlockContextExpression z,
        BlockContextExpression volume,
        BlockContextExpression pitch,
        boolean distanceDelay,
        Optional<HolderSet<Biome>> biomes) implements BlockClientTickable {

  private static final Codec<SoundSource> SOUND_SOURCE_CODEC =
          Codec.STRING.comapFlatMap(s -> DataResult.success(SoundSource.valueOf(s.toLowerCase(Locale.ROOT))),
                  s -> s.getName().toLowerCase(Locale.ROOT));

    public static final Codec<BlockSoundEmitter> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("sound").forGetter(BlockSoundEmitter::sound),
            SOUND_SOURCE_CODEC.optionalFieldOf("source", SoundSource.BLOCKS).forGetter(BlockSoundEmitter::category),
            BlockContextExpression.CODEC.optionalFieldOf("chance", BlockContextExpression.ONE).forGetter(BlockSoundEmitter::chance),
            BlockContextExpression.CODEC.optionalFieldOf("x", BlockContextExpression.ZERO).forGetter(BlockSoundEmitter::x),
            BlockContextExpression.CODEC.optionalFieldOf("y", BlockContextExpression.ZERO).forGetter(BlockSoundEmitter::y),
            BlockContextExpression.CODEC.optionalFieldOf("z", BlockContextExpression.ZERO).forGetter(BlockSoundEmitter::z),
            BlockContextExpression.CODEC.optionalFieldOf("volume", BlockContextExpression.ZERO).forGetter(BlockSoundEmitter::volume),
            BlockContextExpression.CODEC.optionalFieldOf("pitch", BlockContextExpression.ZERO).forGetter(BlockSoundEmitter::pitch),
            Codec.BOOL.optionalFieldOf("distance_delay", false).forGetter(BlockSoundEmitter::distanceDelay),
            RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(BlockSoundEmitter::biomes)
    ).apply(i, BlockSoundEmitter::new));


    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        double spawnChance = chance.getValue(level, pos, state);
        if (level.random.nextFloat() < spawnChance) {
            if (biomes.isPresent()) {
                var biome = level.getBiome(pos);
                if (!biomes.get().contains(biome)) return;
            }

            Vec3 vec = pos.getCenter().add(
                    x.getValue(level, pos, state),
                    y.getValue(level, pos, state),
                    z.getValue(level, pos, state));

            float v = (float) volume.getValue(level, pos, state);
            float p = (float) pitch.getValue(level, pos, state);

            level.playLocalSound( vec.x, vec.y, vec.z,
                    sound, category, v, p, false);
        }
    }


}
