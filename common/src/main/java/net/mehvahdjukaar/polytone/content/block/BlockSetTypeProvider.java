package net.mehvahdjukaar.polytone.content.block;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.Optional;

public interface BlockSetTypeProvider {

    Codec<BlockSetTypeProvider> CODEC = SchemaCodecs.referenceOrDirect(
            SchemaCodecs.alternatives(
                    "registered", Polytone.BLOCK_SET.byNameCodec(),
                    "vanilla", Vanilla.CODEC,
                    "wood", VanillaWood.CODEC), Custom.CODEC);

    BlockSetType getOrCreate(BlockSetType original, Optional<SoundType> customSound);

    WoodType getOrCreateWood(WoodType original, Optional<SoundType> customSound);

    record Vanilla(BlockSetType vanilla) implements BlockSetTypeProvider {

        private static final Codec<BlockSetTypeProvider.Vanilla> CODEC = BlockSetType.CODEC.xmap(
                Vanilla::new, Vanilla::vanilla);

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


        @Override
        public WoodType getOrCreateWood(WoodType original, Optional<SoundType> customSound) {
            //find wood type with same block type
            for (var w : WoodType.values().toList()) {
                if (w.setType() == vanilla && (customSound.isEmpty() || w.soundType() == customSound.get())) {
                    return w;
                }
            }

            BlockSetType blockSetType = this.getOrCreate(original.setType(), customSound);
            return new WoodType(blockSetType.name(),
                    blockSetType,
                    customSound.orElse(original.soundType()),
                    customSound.orElse(original.hangingSignSoundType()),
                    original.fenceGateOpen(),
                    original.fenceGateClose()
            );
        }
    }

    record VanillaWood(WoodType vanillaWood) implements BlockSetTypeProvider {
        private static final Codec<BlockSetTypeProvider.VanillaWood> CODEC = WoodType.CODEC.xmap(
                VanillaWood::new, VanillaWood::vanillaWood);

        @Override
        public BlockSetType getOrCreate(BlockSetType original, Optional<SoundType> customSound) {
            return new Vanilla(vanillaWood.setType()).getOrCreate(original, customSound);
        }

        @Override
        public WoodType getOrCreateWood(WoodType original, Optional<SoundType> customSound) {
            if (customSound.isEmpty()) {
                return vanillaWood;
            }
            var set = new Vanilla(vanillaWood.setType()).getOrCreate(original.setType(), customSound);
            return new WoodType(
                    set.name(),
                    set,
                    customSound.orElse(vanillaWood.soundType()),
                    customSound.orElse(vanillaWood.hangingSignSoundType()),
                    vanillaWood.fenceGateOpen(),
                    vanillaWood.fenceGateClose()
            );
        }
    }

    //both sound type and block type
    record Custom(Optional<SoundEvent> doorClose, Optional<SoundEvent> doorOpen,
                  Optional<SoundEvent> trapdoorClose, Optional<SoundEvent> trapdoorOpen,
                  Optional<SoundEvent> pressurePlateClickOff, Optional<SoundEvent> pressurePlateClickOn,
                  Optional<SoundEvent> buttonClickOff,
                  Optional<SoundEvent> buttonClickOn,
                  Optional<SoundEvent> fanceGateOpen,
                  Optional<SoundEvent> fanceGateClose
    ) implements BlockSetTypeProvider {

        public static final SchemaCodec<Custom> CODEC = SchemaRecord.create(Custom.class, (i) -> i.group(
                i.optional("door_close", CodecUtils.forwardAwareSoundEvent(), Custom::doorClose),
                i.optional("door_open", CodecUtils.forwardAwareSoundEvent(), Custom::doorOpen),
                i.optional("trapdoor_close", CodecUtils.forwardAwareSoundEvent(), Custom::trapdoorClose),
                i.optional("trapdoor_open", CodecUtils.forwardAwareSoundEvent(), Custom::trapdoorOpen),
                i.optional("pressure_plate_click_off", CodecUtils.forwardAwareSoundEvent(), Custom::pressurePlateClickOff),
                i.optional("pressure_plate_click_on", CodecUtils.forwardAwareSoundEvent(), Custom::pressurePlateClickOn),
                i.optional("button_click_off", CodecUtils.forwardAwareSoundEvent(), Custom::buttonClickOff),
                i.optional("button_click_on", CodecUtils.forwardAwareSoundEvent(), Custom::buttonClickOn),
                i.optional("fence_gate_open", CodecUtils.forwardAwareSoundEvent(), Custom::fanceGateOpen),
                i.optional("fence_gate_close", CodecUtils.forwardAwareSoundEvent(), Custom::fanceGateClose)
        ).apply(i, Custom::new));


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

        @Override
        public WoodType getOrCreateWood(WoodType original, Optional<SoundType> customSound) {
            BlockSetType blockSetType = this.getOrCreate(original.setType(), customSound);
            return new WoodType(blockSetType.name(),
                    blockSetType,
                    customSound.orElse(original.soundType()),
                    customSound.orElse(original.hangingSignSoundType()),
                    fanceGateOpen.orElse(original.fenceGateOpen()),
                    fanceGateClose.orElse(original.fenceGateClose())
            );
        }
    }

}

