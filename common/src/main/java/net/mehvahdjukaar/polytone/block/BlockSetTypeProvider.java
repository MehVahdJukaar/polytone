package net.mehvahdjukaar.polytone.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.Optional;

public interface BlockSetTypeProvider {

    Codec<BlockSetTypeProvider> CODEC = CodecUtils.referenceOrDirect(
            Polytone.BLOCK_SET.byNameCodec(), Custom.CODEC);

    BlockSetType getOrCreate(BlockSetType original, Optional<SoundType> customSound);

    record Vanilla(BlockSetType vanilla) implements BlockSetTypeProvider {

        @Override
        public BlockSetType getOrCreate(BlockSetType original, Optional<SoundType> customSound) {
            if (customSound.isEmpty() && original.canOpenByHand() == vanilla.canOpenByHand()) {
                //we can return vanilla if no custom sound and no change in canOpenByHand
                return vanilla;
            }
            return new BlockSetType(
                    Polytone.BLOCK_SET.getNextName(),
                    original.canOpenByHand(), //always creates a new one because of this...
                    original.canOpenByWindCharge(),
                    original.canButtonBeActivatedByArrows(),
                    original.pressurePlateSensitivity(),
                    customSound.orElse(vanilla.soundType()),
                    vanilla.doorClose(),
                    vanilla.doorOpen(),
                    vanilla.trapdoorClose(),
                    vanilla.trapdoorOpen(),
                    vanilla.pressurePlateClickOff(),
                    vanilla.pressurePlateClickOn(),
                    vanilla.buttonClickOff(),
                    vanilla.buttonClickOn()
            );
        }
    }

    record Custom(Optional<SoundEvent> doorClose, Optional<SoundEvent> doorOpen,
                  Optional<SoundEvent> trapdoorClose, Optional<SoundEvent> trapdoorOpen,
                  Optional<SoundEvent> pressurePlateClickOff, Optional<SoundEvent> pressurePlateClickOn,
                  Optional<SoundEvent> buttonClickOff,
                  Optional<SoundEvent> buttonClickOn) implements BlockSetTypeProvider {

        public static final Codec<Custom> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                CodecUtils.forwardAwareSoundEvent().optionalFieldOf("door_close").forGetter(Custom::doorClose),
                CodecUtils.forwardAwareSoundEvent().optionalFieldOf("door_open").forGetter(Custom::doorOpen),
                CodecUtils.forwardAwareSoundEvent().optionalFieldOf("trapdoor_close").forGetter(Custom::trapdoorClose),
                CodecUtils.forwardAwareSoundEvent().optionalFieldOf("trapdoor_open").forGetter(Custom::trapdoorOpen),
                CodecUtils.forwardAwareSoundEvent().optionalFieldOf("pressure_plate_click_off").forGetter(Custom::pressurePlateClickOff),
                CodecUtils.forwardAwareSoundEvent().optionalFieldOf("pressure_plate_click_on").forGetter(Custom::pressurePlateClickOn),
                CodecUtils.forwardAwareSoundEvent().optionalFieldOf("button_click_off").forGetter(Custom::buttonClickOff),
                CodecUtils.forwardAwareSoundEvent().optionalFieldOf("button_click_on").forGetter(Custom::buttonClickOn)
        ).apply(instance, Custom::new));

        @Override
        public BlockSetType getOrCreate(BlockSetType original, Optional<SoundType> customSound) {
            return new BlockSetType(
                    Polytone.BLOCK_SET.getNextName(),
                    original.canOpenByHand(),
                    original.canOpenByWindCharge(),
                    original.canButtonBeActivatedByArrows(),
                    original.pressurePlateSensitivity(),
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

