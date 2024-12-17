package net.mehvahdjukaar.polytone.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.color.MapColorHelper;
import net.mehvahdjukaar.polytone.sound.PolytoneSoundType;
import net.mehvahdjukaar.polytone.utils.ListUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public record ServerBlockModifier(Optional<SoundType> soundType,
                                  Optional<Function<BlockState, MapColor>> mapColor) {

    //offset
    public static final Codec<ServerBlockModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PolytoneSoundType.CODEC.optionalFieldOf("sound_type").forGetter(ServerBlockModifier::soundType),
            MapColorHelper.CODEC.xmap(c -> (Function<BlockState, MapColor>) (a) -> c, f -> MapColor.NONE).optionalFieldOf(
                    "map_color").forGetter(ServerBlockModifier::mapColor)
    ).apply(instance, ServerBlockModifier::new));

}
