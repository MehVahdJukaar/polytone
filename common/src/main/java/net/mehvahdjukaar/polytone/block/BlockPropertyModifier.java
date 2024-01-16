package net.mehvahdjukaar.polytone.block;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.color.MapColorHelper;
import net.mehvahdjukaar.polytone.colormap.TintColorGetter;
import net.mehvahdjukaar.polytone.sound.SoundTypesManager;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public record BlockPropertyModifier(
        Optional<? extends BlockColor> tintGetter,
        Optional<SoundType> soundType,
        Optional<Function<BlockState, MaterialColor>> mapColor
        //Optional<Boolean> canOcclude,
        //Optional<Object> spawnParticlesOnBreak,
        //Optional<Boolean> viewBlocking,
        //Optional<Object> emissiveRendering)
){

    public BlockPropertyModifier merge(BlockPropertyModifier other) {
        return new BlockPropertyModifier(
                this.tintGetter,
                other.soundType().isPresent() ? other.soundType() : this.soundType(),
                other.mapColor.isPresent() ? other.mapColor() : this.mapColor()
                //other.canOcclude().isPresent() ? other.canOcclude() : this.canOcclude(),
                //other.spawnParticlesOnBreak().isPresent() ? other.spawnParticlesOnBreak() : this.spawnParticlesOnBreak(),
                // other.viewBlocking().isPresent() ? other.viewBlocking() : this.viewBlocking(),
                //other.emissiveRendering().isPresent() ? other.emissiveRendering() : this.emissiveRendering(),

        );
    }

    public static BlockPropertyModifier ofColor(TintColorGetter colormap) {
        return new BlockPropertyModifier(Optional.of(colormap), Optional.empty(), Optional.empty());
    }

    // returns the old ones
    public BlockPropertyModifier apply(Block block) {
        SoundType oldSound = null;
        if (soundType.isPresent()) {
            oldSound = block.soundType;
            block.soundType = soundType.get();
        }
        Function<BlockState, MaterialColor> oldMapColor = null;
        if (mapColor.isPresent()) {
            oldMapColor = block.properties.materialColor;
            block.properties.materialColor = mapColor.get();
            for (var s : block.getStateDefinition().getPossibleStates()) {
                s.materialColor = block.properties.materialColor.apply(s);
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
                Optional.ofNullable(oldMapColor));
    }


    public static final Decoder<BlockPropertyModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    StrOpt.of(TintColorGetter.CODEC, "colormap").forGetter(b -> b.tintGetter.flatMap(t -> Optional.ofNullable(t instanceof TintColorGetter c ? c : null))),
                    StrOpt.of(SoundTypesManager.CODEC, "sound_type").forGetter(BlockPropertyModifier::soundType),
                    StrOpt.of(MapColorHelper.CODEC.xmap(c -> (Function<BlockState, MaterialColor>) (a) -> c, f -> MaterialColor.NONE),
                            "map_color").forGetter(BlockPropertyModifier::mapColor)
                    // Codec.BOOL.optionalFieldOf("can_occlude").forGetter(ClientBlockProperties::canOcclude),
                    //Codec.BOOL.optionalFieldOf("spawn_particles_on_break").forGetter(c -> c.spawnParticlesOnBreak.flatMap(o -> Optional.ofNullable(o instanceof Boolean b ? b : null))),
                    // Codec.BOOL.optionalFieldOf("view_blocking").forGetter(ClientBlockProperties::viewBlocking),
                    //Codec.BOOL.optionalFieldOf("emissive_rendering").forGetter(c -> c.emissiveRendering.flatMap(o -> Optional.ofNullable(o instanceof Boolean b ? b : null))),
            ).apply(instance, BlockPropertyModifier::new));


}
