package net.mehvahdjukaar.polytone.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.color.MapColorHelper;
import net.mehvahdjukaar.polytone.colormap.Colormap;
import net.mehvahdjukaar.polytone.colormap.CompoundBlockColors;
import net.mehvahdjukaar.polytone.particle.ParticleEmitter;
import net.mehvahdjukaar.polytone.sound.SoundTypesManager;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.mehvahdjukaar.polytone.utils.TargetsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public record BlockPropertyModifier(
        Optional<? extends BlockColor> tintGetter,
        Optional<SoundType> soundType,
        Optional<Function<BlockState, MapColor>> mapColor,
        //Optional<Boolean> canOcclude,
        //Optional<Object> spawnParticlesOnBreak,
        //Optional<Boolean> viewBlocking,
        //Optional<Object> emissiveRendering,
        Optional<ToIntFunction<BlockState>> clientLight,
        Optional<List<ParticleEmitter>> particleEmitters,
        Optional<BlockBehaviour.OffsetFunction> offsetType,
        Optional<Set<ResourceLocation>> explicitTargets) {

    // Other has priority
    public BlockPropertyModifier merge(BlockPropertyModifier other) {
        return new BlockPropertyModifier(
                other.tintGetter.isPresent() ? other.tintGetter() : this.tintGetter(),
                other.soundType().isPresent() ? other.soundType() : this.soundType(),
                other.mapColor.isPresent() ? other.mapColor() : this.mapColor(),
                //other.canOcclude().isPresent() ? other.canOcclude() : this.canOcclude(),
                //other.spawnParticlesOnBreak().isPresent() ? other.spawnParticlesOnBreak() : this.spawnParticlesOnBreak(),
                // other.viewBlocking().isPresent() ? other.viewBlocking() : this.viewBlocking(),
                //other.emissiveRendering().isPresent() ? other.emissiveRendering() : this.emissiveRendering(),
                other.clientLight.isPresent() ? other.clientLight : this.clientLight,
                other.particleEmitters.isPresent() ? other.particleEmitters : this.particleEmitters,
                other.offsetType().isPresent() ? other.offsetType() : this.offsetType(),
                TargetsHelper.merge(other.explicitTargets, this.explicitTargets)
        );
    }

    public static BlockPropertyModifier ofColor(BlockColor colormap) {
        return new BlockPropertyModifier(Optional.of(colormap),
                java.util.Optional.empty(), java.util.Optional.empty(),
                java.util.Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    // returns the old ones
    public BlockPropertyModifier apply(Block block) {
        SoundType oldSound = null;
        if (soundType.isPresent()) {
            oldSound = block.soundType;
            block.soundType = soundType.get();
        }

        Optional<BlockBehaviour.OffsetFunction> oldOffsetType = Optional.empty();
        boolean hasOffset = false;
        if (offsetType.isPresent()) {
            oldOffsetType = block.defaultBlockState().offsetFunction;
            for (var s : block.getStateDefinition().getPossibleStates()) {
                s.offsetFunction = offsetType;
                hasOffset = true;
            }
        }
        if (hasOffset) block.dynamicShape = true;

        Function<BlockState, MapColor> oldMapColor = null;
        if (mapColor.isPresent()) {
            oldMapColor = block.properties.mapColor;
            block.properties.mapColor = mapColor.get();
            for (var s : block.getStateDefinition().getPossibleStates()) {
                s.mapColor = block.properties.mapColor.apply(s);
            }
        }

        ToIntFunction<BlockState> oldClientLight = null;
        if (clientLight.isPresent()) {
            oldClientLight = block.properties.lightEmission;
            block.properties.lightEmission = clientLight.get();
            for (var s : block.getStateDefinition().getPossibleStates()) {
                s.lightEmission = block.properties.lightEmission.applyAsInt(s);
            }
        }

        BlockColor color = null;
        if (tintGetter.isPresent()) {
            BlockColors blockColors = Minecraft.getInstance().getBlockColors();
            color = PlatStuff.getBlockColor(blockColors, block);
            blockColors.register(tintGetter.get(), block);
        }


        // returns old properties
        return new BlockPropertyModifier(Optional.ofNullable(color), Optional.ofNullable(oldSound),
                Optional.ofNullable(oldMapColor), Optional.ofNullable(oldClientLight),
                Optional.empty(), oldOffsetType, Optional.empty());
    }


    public static final Decoder<BlockPropertyModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    StrOpt.of(CompoundBlockColors.CODEC, "colormap").forGetter(b -> b.tintGetter.flatMap(t -> java.util.Optional.ofNullable(t instanceof CompoundBlockColors c ? c : null))),
                    //normal opt so it can fail when using modded sounds
                    SoundTypesManager.CODEC.optionalFieldOf("sound_type").forGetter(BlockPropertyModifier::soundType),
                    StrOpt.of(MapColorHelper.CODEC.xmap(c -> (Function<BlockState, MapColor>) (a) -> c, f -> MapColor.NONE),
                            "map_color").forGetter(BlockPropertyModifier::mapColor),
                    // Codec.BOOL.optionalFieldOf("can_occlude").forGetter(ClientBlockProperties::canOcclude),
                    //Codec.BOOL.optionalFieldOf("spawn_particles_on_break").forGetter(c -> c.spawnParticlesOnBreak.flatMap(o -> Optional.ofNullable(o instanceof Boolean b ? b : null))),
                    // Codec.BOOL.optionalFieldOf("view_blocking").forGetter(ClientBlockProperties::viewBlocking),
                    //Codec.BOOL.optionalFieldOf("emissive_rendering").forGetter(c -> c.emissiveRendering.flatMap(o -> Optional.ofNullable(o instanceof Boolean b ? b : null))),
                    StrOpt.of(Codec.intRange(0, 15).xmap(integer -> (ToIntFunction<BlockState>) s -> integer, toIntFunction -> 0),
                            "client_light").forGetter(BlockPropertyModifier::clientLight),
                    StrOpt.of(ParticleEmitter.CODEC.listOf(), "particle_emitters").forGetter(BlockPropertyModifier::particleEmitters),
                    StrOpt.of(StringRepresentable.fromEnum(BlockPropertyModifier.OffsetTypeR::values)
                                    .xmap(OffsetTypeR::getFunction, offsetFunction -> OffsetTypeR.NONE),
                            "offset_type").forGetter(BlockPropertyModifier::offsetType),
                    StrOpt.of(TargetsHelper.CODEC, "targets").forGetter(BlockPropertyModifier::explicitTargets)
            ).apply(instance, BlockPropertyModifier::new));


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


    public static BlockPropertyModifier fromOfProperties(Properties properties) {
        Set<ResourceLocation> set = null;
        Colormap colormap;
        var targets = properties.getProperty("blocks");
        if (targets != null) {
            set = Arrays.stream(targets.split(" "))
                    .map(ResourceLocation::new)
                    .collect(Collectors.toSet());
        }
        String format = properties.getProperty("format");
        Integer col = null;
        String singleColor = properties.getProperty("color");
        if(singleColor != null){
            col = Integer.parseInt(singleColor, 16);
        }
        if ("fixed".equals(format)) {
            colormap = Colormap.fixed();
        } else if ("grid".equals(format)) {
            colormap = Colormap.biomeId();
        } else {
            colormap = Colormap.defTriangle();
        }
        if(col != null) {
            int[][] matrix = {{col}};
            colormap.acceptTexture(new ArrayImage(matrix));
        }
        return new BlockPropertyModifier(Optional.of(colormap),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.ofNullable(set));
    }

}
