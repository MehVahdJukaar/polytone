package net.mehvahdjukaar.polytone.content.block;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.color.MapColorHelper;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.content.colormap.IndexCompoundColorGetter;
import net.mehvahdjukaar.polytone.content.particle.BlockParticleEmitter;
import net.mehvahdjukaar.polytone.content.sound.BlockSoundEmitter;
import net.mehvahdjukaar.polytone.content.sound.PolytoneSoundType;
import net.mehvahdjukaar.polytone.common.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static net.mehvahdjukaar.polytone.common.struc.ListUtils.mergeList;

public record BlockPropertyModifier(
        Optional<? extends BlockColor> tintGetter,
        Optional<SoundType> soundType,
        Optional<Function<BlockState, MapColor>> mapColor,
        Optional<Boolean> canOcclude,
        Optional<Boolean> spawnParticlesOnBreak,
        Optional<Boolean> breakingParticlesTinted,
        Optional<ChunkSectionLayer> renderType,
        Optional<ToIntFunction<BlockState>> clientLight,
        List<BlockParticleEmitter> particleEmitters,
        List<BlockSoundEmitter> soundEmitters,
        Optional<BlockBehaviour.OffsetFunction> offsetType,
        Optional<BlockSetTypeProvider> blockSetType,
        Boolean disableParticles,
        @NotNull Targets targets,
        boolean tintHack) {
    public BlockPropertyModifier merge(BlockPropertyModifier newMod) {
        return new BlockPropertyModifier(
                newMod.tintGetter.isPresent() ? newMod.tintGetter() : this.tintGetter(),
                newMod.soundType().isPresent() ? newMod.soundType() : this.soundType(),
                newMod.mapColor.isPresent() ? newMod.mapColor() : this.mapColor(),
                newMod.canOcclude().isPresent() ? newMod.canOcclude() : this.canOcclude(),
                newMod.spawnParticlesOnBreak().isPresent() ? newMod.spawnParticlesOnBreak() : this.spawnParticlesOnBreak(),
                newMod.breakingParticlesTinted().isPresent() ? newMod.breakingParticlesTinted() : this.breakingParticlesTinted(),
                newMod.renderType().isPresent() ? newMod.renderType() : this.renderType(),
                newMod.clientLight.isPresent() ? newMod.clientLight : this.clientLight,
                mergeList(newMod.particleEmitters, this.particleEmitters),
                mergeList(newMod.soundEmitters, this.soundEmitters),
                newMod.offsetType().isPresent() ? newMod.offsetType() : this.offsetType(),
                newMod.blockSetType().isPresent() ? newMod.blockSetType() : this.blockSetType(),
                newMod.disableParticles || this.disableParticles,
                newMod.targets.merge(this.targets),
                newMod.tintHack || this.tintHack
        );
    }

    public static BlockPropertyModifier ofBlockColor(BlockColor colormap) {
        return new BlockPropertyModifier(Optional.ofNullable(colormap),
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), List.of(),
                List.of(), Optional.empty(), Optional.empty(),
                false, Targets.EMPTY, false);
    }

    public static BlockPropertyModifier coloringBlocks(BlockColor colormap, Block... blocks) {
        return coloringBlocks(colormap, Set.of(Arrays.stream(blocks).map(BuiltInRegistries.BLOCK::getKey).toArray(Identifier[]::new)));
    }

    public static BlockPropertyModifier coloringBlocks(BlockColor colormap, List<Block> blocks) {
        return coloringBlocks(colormap, blocks.stream().map(BuiltInRegistries.BLOCK::getKey).collect(Collectors.toSet()));
    }

    public static BlockPropertyModifier coloringBlocks(BlockColor colormap, Set<Identifier> blocks) {
        Targets t = Targets.ofIds(blocks);
        return new BlockPropertyModifier(Optional.of(colormap),
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                java.util.Optional.empty(), Optional.empty(), List.of(),
                List.of(), Optional.empty(), Optional.empty(),
                false, t, false);
    }

    // returns the old ones
    public BlockPropertyModifier apply(Block block) {

        SoundType oldSound = null;
        if (soundType.isPresent()) {
            oldSound = block.soundType;
            block.soundType = soundType.get();
        }


        Function<BlockState, MapColor> oldMapColor = null;
        if (mapColor.isPresent()) {
            oldMapColor = block.properties.mapColor;
            block.properties.mapColor = mapColor.get();
            for (var s : block.getStateDefinition().getPossibleStates()) {
                s.mapColor = block.properties.mapColor.apply(s);
            }
        }

        Boolean oldCanOcclude = null;
        if (canOcclude.isPresent()) {
            oldCanOcclude = block.properties.canOcclude;
            block.properties.canOcclude = canOcclude.get();
            for (var s : block.getStateDefinition().getPossibleStates()) {
                s.canOcclude = canOcclude.get();
                s.initCache(); //recalculate cache
            }
        }

        Boolean oldSpawnParticlesOnBreak = null;
        if (spawnParticlesOnBreak.isPresent()) {
            oldSpawnParticlesOnBreak = block.properties.spawnTerrainParticles;
            block.properties.spawnTerrainParticles = spawnParticlesOnBreak.get();
            for (var s : block.getStateDefinition().getPossibleStates()) {
                s.spawnTerrainParticles = block.properties.spawnTerrainParticles;
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

        BlockColor oldColor = null;
        if (tintGetter.isPresent()) {
            BlockColors blockColors = Minecraft.getInstance().getBlockColors();
            oldColor = PlatStuff.getBlockColor(blockColors, block);
            BlockColor blockColor = tintGetter.get();
            if (blockColor instanceof IColorGetter cg) {
                blockColor = Polytone.COLORMAPS.getOrCreateConcurrentColormap(cg);
            }
            blockColors.register(blockColor, block);
            Polytone.BLOCK_MODIFIERS.maybeAssignToDefaultGrassAndFoliage(block, blockColor);
        }

        BlockSetTypeProvider oldType = null;
        if (blockSetType.isPresent()) {
            BlockSetTypeProvider provider = blockSetType.get();
            if (block instanceof DoorBlock db) {
                oldType = new BlockSetTypeProvider.Vanilla(db.type);
                db.type = provider.getOrCreate(db.type(), soundType);
            } else if (block instanceof TrapDoorBlock tb) {
                oldType = new BlockSetTypeProvider.Vanilla(tb.type);
                tb.type = provider.getOrCreate(tb.type, soundType);
            } else if (block instanceof ButtonBlock bb) {
                oldType = new BlockSetTypeProvider.Vanilla(bb.type);
                bb.type = provider.getOrCreate(bb.type, soundType);
            } else if (block instanceof PressurePlateBlock ppb) {
                oldType = new BlockSetTypeProvider.Vanilla(ppb.type);
                ppb.type = provider.getOrCreate(ppb.type, soundType);
            } else if (block instanceof FenceGateBlock fg) {
                oldType = new BlockSetTypeProvider.VanillaWood(fg.type);
                fg.type = provider.getOrCreateWood(fg.type, soundType);
            } else if (block instanceof SignBlock sb) {
                oldType = new BlockSetTypeProvider.VanillaWood(sb.type);
                sb.type = provider.getOrCreateWood(sb.type, soundType);
            }
        }
        if (tintHack) {
            //Polytone.VARIANT_TEXTURES.addTintOverrideHack(block);
        }

        ChunkSectionLayer oldRenderType = null;
        if (renderType.isPresent()) {
            oldRenderType = PlatStuff.getRenderType(block);
            ChunkSectionLayer o = renderType.get();
            PlatStuff.setRenderType(block, o);
        }

        // returns old properties
        return new BlockPropertyModifier(Optional.ofNullable(oldColor), Optional.ofNullable(oldSound),
                Optional.ofNullable(oldMapColor),
                Optional.ofNullable(oldCanOcclude), Optional.ofNullable(oldSpawnParticlesOnBreak),
                Optional.empty(),
                Optional.ofNullable(oldRenderType), Optional.ofNullable(oldClientLight),
                List.of(), List.of(), Optional.empty(),
                Optional.ofNullable(oldType),
                false, Targets.EMPTY, false);
    }

    // Declaration-site schema: enum dropdown over the layer labels - inference only sees
    // STRING.xmap and would render plain text.
    public static final Codec<ChunkSectionLayer> SECTION_LAYER_CODEC =
            net.mehvahdjukaar.codecui.SchemaCodec.of(
                    Codec.STRING.xmap(s -> ChunkSectionLayer.valueOf(s.toUpperCase(Locale.ROOT)), ChunkSectionLayer::label),
                    new net.mehvahdjukaar.codecui.Schema.Enum<>(
                            List.of(ChunkSectionLayer.values()), ChunkSectionLayer::label));

    public static final SchemaCodec<BlockPropertyModifier> CODEC = SchemaRecord.create(BlockPropertyModifier.class, i ->
            i.group(
                    i.optional("colormap", IndexCompoundColorGetter.SINGLE_OR_MULTIPLE, b -> b.tintGetter.flatMap(t -> java.util.Optional.ofNullable(t instanceof IndexCompoundColorGetter c ? c : null))),
                    //normal opt so it can fail when using modded sounds
                    i.optional("sound_type", PolytoneSoundType.CODEC, BlockPropertyModifier::soundType),
                    i.optional("map_color", MapColorHelper.CODEC.xmap(c -> (Function<BlockState, MapColor>) (a) -> c, f -> MapColor.NONE), BlockPropertyModifier::mapColor),
                    i.optional("can_occlude", Codec.BOOL, BlockPropertyModifier::canOcclude),
                    i.optional("spawn_particles_on_break", Codec.BOOL, BlockPropertyModifier::spawnParticlesOnBreak),
                    i.optional("tinted_breaking_particles", Codec.BOOL, BlockPropertyModifier::breakingParticlesTinted),
                    i.optional("render_type", SECTION_LAYER_CODEC, BlockPropertyModifier::renderType),
                    i.optional("client_light", Codec.intRange(0, 15).xmap(integer -> (ToIntFunction<BlockState>) s -> integer, toIntFunction -> 0), BlockPropertyModifier::clientLight),
                    i.optional("particle_emitters", BlockParticleEmitter.CODEC.listOf(), List.of(), BlockPropertyModifier::particleEmitters),
                    i.optional("sound_emitters", BlockSoundEmitter.CODEC.listOf(), List.of(), BlockPropertyModifier::soundEmitters),
                    i.optional("offset_type", BlockOffsets.CODEC, BlockPropertyModifier::offsetType),
                    i.optional("block_set_type", BlockSetTypeProvider.CODEC, BlockPropertyModifier::blockSetType),
                    i.optional("disable_particles", Codec.BOOL, false, BlockPropertyModifier::disableParticles),
                    i.optional("targets", Targets.CODEC, Targets.EMPTY, BlockPropertyModifier::targets),
                    //dont use
                    i.optional("force_tint_hack", Codec.BOOL, false, BlockPropertyModifier::tintHack)
            ).apply(i, BlockPropertyModifier::new));

    public static final SchemaCodec<BlockPropertyModifier> PARTIAL_CODEC = SchemaRecord.create(BlockPropertyModifier.class, i ->
            i.group(
                    i.optional("colormap", IndexCompoundColorGetter.SINGLE_OR_MULTIPLE, b -> b.tintGetter.flatMap(t -> java.util.Optional.ofNullable(t instanceof IndexCompoundColorGetter c ? c : null)))
            ).apply(i, c -> ofBlockColor(c.orElse(null))));

    public boolean hasColormap() {
        return this.tintGetter.isPresent();
    }

    @Nullable
    public IColorGetter getColormap() {
        return (IColorGetter) tintGetter.orElse(null);
    }

}
