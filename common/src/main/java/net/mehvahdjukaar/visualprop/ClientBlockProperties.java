package net.mehvahdjukaar.visualprop;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public record ClientBlockProperties(
        Block target,
        Optional<SoundType> soundType,
        Optional<Function<BlockState, MapColor>> mapColor,
        //Optional<Boolean> canOcclude,
        //Optional<Object> spawnParticlesOnBreak,
        //Optional<Boolean> viewBlocking,
        //Optional<Object> emissiveRendering,
        Optional<BlockBehaviour.OffsetFunction> offsetType) {

    public ClientBlockProperties merge(ClientBlockProperties other) {
        return new ClientBlockProperties(
                this.target,
                other.soundType().isPresent() ? other.soundType() : this.soundType(),
                other.mapColor.isPresent() ? other.mapColor() : this.mapColor(),
                //other.canOcclude().isPresent() ? other.canOcclude() : this.canOcclude(),
                //other.spawnParticlesOnBreak().isPresent() ? other.spawnParticlesOnBreak() : this.spawnParticlesOnBreak(),
                // other.viewBlocking().isPresent() ? other.viewBlocking() : this.viewBlocking(),
                //other.emissiveRendering().isPresent() ? other.emissiveRendering() : this.emissiveRendering(),
                other.offsetType().isPresent() ? other.offsetType() : this.offsetType()
        );
    }

    // returns the old ones
    public ClientBlockProperties apply() {
        SoundType oldSound = null;
        if (soundType.isPresent()) {
            oldSound = target.soundType;
            target.soundType = soundType.get();
        }
        Optional<BlockBehaviour.OffsetFunction> oldOffsetType = Optional.empty();
        if (offsetType.isPresent()) {
            oldOffsetType = target.defaultBlockState().offsetFunction;
            for (var s : target.getStateDefinition().getPossibleStates()) {
                s.offsetFunction = offsetType;
            }
        }
        Function<BlockState, MapColor> oldMapColor = null;
        if (mapColor.isPresent()) {
            oldMapColor = target.properties.mapColor;
            target.properties.mapColor = mapColor.get();
        }


        return new ClientBlockProperties(this.target, Optional.ofNullable(oldSound), Optional.ofNullable(oldMapColor), oldOffsetType);
    }


    public static final Decoder<ClientBlockProperties> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BuiltInRegistries.BLOCK.byNameCodec().fieldOf("target").forGetter(ClientBlockProperties::block),
                    SoundTypeHelper.CODEC.optionalFieldOf("sound_event").forGetter(ClientBlockProperties::soundType),
                    MapColorHelper.CODEC.xmap(c -> (Function<BlockState, MapColor>) (a) -> c, f -> MapColor.NONE)
                            .optionalFieldOf("map_color").forGetter(ClientBlockProperties::mapColor),
                    // Codec.BOOL.optionalFieldOf("can_occlude").forGetter(ClientBlockProperties::canOcclude),
                    //Codec.BOOL.optionalFieldOf("spawn_particles_on_break").forGetter(c -> c.spawnParticlesOnBreak.flatMap(o -> Optional.ofNullable(o instanceof Boolean b ? b : null))),
                    // Codec.BOOL.optionalFieldOf("view_blocking").forGetter(ClientBlockProperties::viewBlocking),
                    //Codec.BOOL.optionalFieldOf("emissive_rendering").forGetter(c -> c.emissiveRendering.flatMap(o -> Optional.ofNullable(o instanceof Boolean b ? b : null))),
                    StringRepresentable.fromEnum(ClientBlockProperties.OffsetTypeR::values)
                            .xmap(OffsetTypeR::getFunction, offsetFunction -> OffsetTypeR.NONE)
                            .optionalFieldOf("offset_type").forGetter(ClientBlockProperties::offsetType)
            ).apply(instance, ClientBlockProperties::new));

    public Block block() {
        return null;
    }


    public enum OffsetTypeR implements StringRepresentable {
        NONE(BlockBehaviour.OffsetType.NONE),
        XZ(BlockBehaviour.OffsetType.XZ),
        XYZ(BlockBehaviour.OffsetType.XYZ);

        private final BlockBehaviour.OffsetType original;

        OffsetTypeR(BlockBehaviour.OffsetType offsetType) {
            this.original = offsetType;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public BlockBehaviour.OffsetFunction getFunction() {
            var p = BlockBehaviour.Properties.of().offsetType(original);
            return p.offsetFunction.orElse((blockState, blockGetter, blockPos) -> Vec3.ZERO);
        }
    }

}
