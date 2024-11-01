package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level {

    protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }

    @WrapOperation(method = "doAnimateTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    public void polytone$extraParticles(Block instance, BlockState state, Level level, BlockPos pos, RandomSource random,
                                        Operation<Void> original) {
        original.call(instance, state, level, pos, random);

        Polytone.BLOCK_MODIFIERS.maybeEmitParticle(instance, state, level, pos);
    }

    @WrapOperation(method = "getSkyColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 polytone$modifySkyColor(Vec3 center, CubicSampler.Vec3Fetcher fetcher,
                                         Operation<Vec3> original) {
        Vec3 modified = Polytone.DIMENSION_MODIFIERS.modifySkyColor(center, (ClientLevel) (Object) this);
        if (modified != null) return modified;
        return original.call(center, fetcher);
    }


    @ModifyExpressionValue(method = "getSkyColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private float polytone$modifyRainSkyDarken(float original) {
        if (original != 0 && Polytone.DIMENSION_MODIFIERS.shouldCancelSkyWeatherDarken(this)) {
            return 0;
        }
        return original;
    }

    @ModifyExpressionValue(method = "getSkyColor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getThunderLevel(F)F"))
    private float polytone$modifyThunderSkyDarken(float original) {
        if (original != 0 && Polytone.DIMENSION_MODIFIERS.shouldCancelSkyWeatherDarken(this)) {
            return 0;
        }
        return original;
    }
}
