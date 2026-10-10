package net.mehvahdjukaar.polytone.compat;

import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

//this is absolutely horrible. Known assumptions or outright wrong issues DH has:
//- keeps biome holders forever, so old world biomes after a rejoin, VERY BAD!!
//- asks color at 0,0 once per block+biome and caches it. BREAKS SWAMP! even in vanilla!
//this means that adding any pack that changes colors or even datapacks that change biome colors will break even in vanilla
public class DistantHorizonsCompat {

    public static void init() {
        DhApiEventRegister.on(DhApiBlockStateWrapperCreatedEvent.class, new AllowColorOverride());
        DhApiEventRegister.on(DhApiBlockColorOverrideEvent.class, new LodTint());
    }

    private static class AllowColorOverride extends DhApiBlockStateWrapperCreatedEvent {
        @Override
        public void blockStateWrapperCreated(DhApiEventParam<EventParam> event) {
            //api is mega weak. we are forced to override any block as we dont know in advance....
            event.value.setAllowApiColorOverride(true);
        }
    }

    //runs on DH threads, once per lod block
    private static class LodTint extends DhApiBlockColorOverrideEvent {
        @Override
        public void onBlockColorOverridden(DhApiEventParam<EventParam> event) {
            EventParam lod = event.value;
            if (!(lod.getBlockStateWrapper().getWrappedMcObject() instanceof BlockState state)) return;
            BlockTintSource tintSource = getTintSource(state);
            if (tintSource == null) return;
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;
            if (!(lod.getBiomeWrapper().getWrappedMcObject() instanceof Holder<?> holder)) return;
            if (!(holder.value() instanceof Biome biome)) return;

            boolean isFromThisLevel = level.registryAccess().lookupOrThrow(Registries.BIOME).getResourceKey(biome).isPresent();
            if (!isFromThisLevel) {
                //DH swallows exceptions here
                var e = new IllegalStateException("Distant Horizons gave Polytone a Biome that is not in the current client level registry (old world or server side biome): " + lod.getBiomeWrapper().getSerialString());
                Minecraft.getInstance().delayCrash(CrashReport.forThrowable(e, "Getting Distant Horizons LOD tint"));
                return;
            }

            BlockPos pos = new BlockPos(lod.getBlockPosX(), lod.getBlockPosY(), lod.getBlockPosZ());
            int tint;
            if (tintSource instanceof IColorGetter colorGetter) {
                tint = colorGetter.sampleColor(level, state, Vec3.atCenterOf(pos), biome, null);
            } else {
                var tintGetter = new PolyLodTintGetter(state, biome, level);
                tint = tintSource.colorInWorld(state, tintGetter, pos);
                //vanilla water only has a particle color
                if (tint == -1) tint = tintSource.colorAsTerrainParticle(state, tintGetter, pos);
            }
            int color = ARGB.multiply(lod.getBaseColorAsInt(), ARGB.opaque(tint));
            lod.setColor(ARGB.alpha(color), ARGB.red(color), ARGB.green(color), ARGB.blue(color));
        }
    }

    @Nullable
    private static BlockTintSource getTintSource(BlockState state) {
        if (state.getBlock() instanceof LiquidBlock) {
            IColorGetter fluidTint = Polytone.FLUID_MODIFIERS.getTint(state.getFluidState().getType());
            if (fluidTint != null) return fluidTint;
        }
        List<BlockTintSource> layers = Minecraft.getInstance().getBlockColors().getTintSources(state);
        return layers.isEmpty() ? null : layers.getFirst();
    }

    // no chunks out there. All we know is the one block and its biome
    private record PolyLodTintGetter(BlockState state, Biome biome, ClientLevel level) implements BlockAndTintGetter {

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver color) {
            return color.getColor(biome, pos.getX(), pos.getZ());
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return state;
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return state.getFluidState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public CardinalLighting cardinalLighting() {
            return CardinalLighting.DEFAULT;
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return LevelLightEngine.EMPTY;
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinY() {
            return level.getMinY();
        }
    }
}
