package net.mehvahdjukaar.polytone.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface BlockSetTypeProvider {

    Codec<BlockSetTypeProvider> REFERENCE_CODEC = ResourceLocation.CODEC.flatXmap(
            id -> Optional.ofNullable(Polytone.BLOCK_SET.get(id)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Could not find a custom Block Set with id " + id +
                            " Did you place it in 'assets/[your pack]/polytone/block_sets/' ?")),
            object -> Optional.ofNullable(Polytone.BLOCK_SET.getKey(object)).map(DataResult::success)
                    .orElse(DataResult.error(() -> "Unknown Block Set: " + object)));

    Codec<BlockSetTypeProvider> CODEC = new ReferenceOrDirectCodec<>(REFERENCE_CODEC, Custom.CODEC);

    BlockSetType getOrCreate(BlockSetType original, Optional<SoundType> customSound);

    record Vanilla(BlockSetType vanilla) implements BlockSetTypeProvider{

        @Override
        public BlockSetType getOrCreate(BlockSetType original,  Optional<SoundType> customSound) {
            return vanilla;
        }
    }

    record Custom(Optional<SoundEvent> doorClose, Optional<SoundEvent> doorOpen,
                  Optional<SoundEvent> trapdoorClose, Optional<SoundEvent> trapdoorOpen,
                  Optional<SoundEvent> pressurePlateClickOff, Optional<SoundEvent> pressurePlateClickOn,
                  Optional<SoundEvent> buttonClickOff, Optional<SoundEvent> buttonClickOn) implements BlockSetTypeProvider{

        public static final Codec<Custom> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                StrOpt.of(BuiltInRegistries.SOUND_EVENT.byNameCodec(), "door_close").forGetter(Custom::doorClose),
                StrOpt.of(BuiltInRegistries.SOUND_EVENT.byNameCodec(), "door_open").forGetter(Custom::doorOpen),
                StrOpt.of(BuiltInRegistries.SOUND_EVENT.byNameCodec(), "trapdoor_close").forGetter(Custom::trapdoorClose),
                StrOpt.of(BuiltInRegistries.SOUND_EVENT.byNameCodec(), "trapdoor_open").forGetter(Custom::trapdoorOpen),
                StrOpt.of(BuiltInRegistries.SOUND_EVENT.byNameCodec(), "pressure_plate_click_off").forGetter(Custom::pressurePlateClickOff),
                StrOpt.of(BuiltInRegistries.SOUND_EVENT.byNameCodec(), "pressure_plate_click_on").forGetter(Custom::pressurePlateClickOn),
                StrOpt.of(BuiltInRegistries.SOUND_EVENT.byNameCodec(), "button_click_off").forGetter(Custom::buttonClickOff),
                StrOpt.of(BuiltInRegistries.SOUND_EVENT.byNameCodec(), "button_click_on").forGetter(Custom::buttonClickOn)
        ).apply(instance, Custom::new));

        @Override
        public BlockSetType getOrCreate(BlockSetType original,  Optional<SoundType> customSound) {
            return new BlockSetType(
                    Polytone.BLOCK_SET.getNextName(),
                    original.canOpenByHand(),
                    customSound.orElse(original.soundType()),
                    doorClose.orElse(original.doorClose()),
                    doorOpen.orElse(original.doorOpen()),
                    trapdoorClose.orElse(original.trapdoorClose()),
                    trapdoorOpen.orElse(original.trapdoorOpen()),
                    pressurePlateClickOff.orElse(original.pressurePlateClickOff()),
                    pressurePlateClickOn.orElse(original.pressurePlateClickOn()),
                    buttonClickOff.orElse(original.buttonClickOff()),
                    buttonClickOn.orElse(original.buttonClickOn())
            );
        }
    }

}

