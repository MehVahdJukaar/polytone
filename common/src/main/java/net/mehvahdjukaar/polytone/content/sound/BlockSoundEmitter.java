package net.mehvahdjukaar.polytone.content.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.block.BlockClientTickable;
import net.mehvahdjukaar.polytone.content.block.TickSource;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

    public static final SchemaCodec<BlockSoundEmitter> CODEC = SchemaRecord.create(BlockSoundEmitter.class, i -> i.group(
            i.field("sound", CodecUtils.forwardAwareSoundEvent(), BlockSoundEmitter::sound),
            i.optional("source", SOUND_SOURCE_CODEC, SoundSource.BLOCKS, BlockSoundEmitter::category),
            i.optional("chance", IBlockExp.CODEC, IBlockExp.ONE, BlockSoundEmitter::chance),
            i.optional("x", IBlockExp.CODEC, IBlockExp.ZERO, BlockSoundEmitter::x),
            i.optional("y", IBlockExp.CODEC, IBlockExp.ZERO, BlockSoundEmitter::y),
            i.optional("z", IBlockExp.CODEC, IBlockExp.ZERO, BlockSoundEmitter::z),
            i.optional("volume", IBlockExp.CODEC, IBlockExp.ZERO, BlockSoundEmitter::volume),
            i.optional("pitch", IBlockExp.CODEC, IBlockExp.ZERO, BlockSoundEmitter::pitch),
            i.optional("distance_delay", Codec.BOOL, false, BlockSoundEmitter::distanceDelay),
            i.field("state_predicate", SchemaCodecs.lenientWithLog(RuleTest.CODEC, "state_predicate", AlwaysTrueTest.INSTANCE), RuleTest.CODEC, BlockSoundEmitter::predicate),
            i.optional("spawn_source", TickSource.CODEC, TickSource.ANIMATE_TICK, BlockSoundEmitter::spawnSource),
            i.optional("biomes", CodecUtils.forwardAwareHomogeneousList(Registries.BIOME), BlockSoundEmitter::biomes)
    ).apply(i, BlockSoundEmitter::new));


    @Override
    public void tick(ClientLevel level, BlockPos pos, BlockState state, TickSource source) {
        if (source != spawnSource) return;
        Vec3 p = pos.getCenter();
        double spawnChance = chance.evaluate(level, p, state);
        if (level.getRandom().nextFloat() < spawnChance) {
            if (biomes.isPresent()) {
                var biome = level.getBiome(pos);
                if (!biomes.get().contains(biome)) return;
            }

            Vec3 vec = pos.getCenter().add(
                    x.evaluate(level, p, state),
                    y.evaluate(level, p, state),
                    z.evaluate(level, p, state));

            float vol = (float) volume.evaluate(level, p, state);
            float pit = (float) pitch.evaluate(level, p, state);

            level.playLocalSound(vec.x, vec.y, vec.z,
                    sound, category, vol, pit, false);
        }
    }


}
