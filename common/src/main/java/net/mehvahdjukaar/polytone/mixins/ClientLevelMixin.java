package net.mehvahdjukaar.polytone.mixins;

import net.minecraft.util.ARGB;
import org.joml.Vector3f;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.block.TickSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientLevel.class, priority = 1100)
public abstract class ClientLevelMixin extends Level {

    protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }

    @WrapOperation(method = "doAnimateTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    public void polytone$extraParticles(Block instance, BlockState state, Level level, BlockPos pos, RandomSource random,
                                        Operation<Void> original) {
        boolean cancels = Polytone.BLOCK_MODIFIERS.runTickers(state,(ClientLevel) level, pos, TickSource.ANIMATE_TICK);
        if (!cancels) {
            original.call(instance, state, level, pos, random);
        }
    }

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    public void polytone$addExtraDestroyParticles(BlockPos pos, BlockState blockState, CallbackInfo ci) {
        if (!blockState.isAir()) {
            //TODO: add more tick sources
            boolean cancels = Polytone.BLOCK_MODIFIERS.runTickers(blockState, (ClientLevel)(Object)this, pos, TickSource.BLOCK_BROKEN);
            if (cancels) {
                ci.cancel();
            }
        }
    }


    // the breaking sound plays in the same method, so a ticker that cancels only stops the particles
    @Inject(method = "addBreakingBlockEffects", at = @At("HEAD"))
    public void polytone$addExtraBreakingParticles(BlockPos pos, Direction direction, boolean playSound, CallbackInfo ci,
                                                   @Share("polytone$cancelParticles") LocalBooleanRef cancelParticles) {
        BlockState state = this.getBlockState(pos);
        if (!state.isAir()) {
            cancelParticles.set(Polytone.BLOCK_MODIFIERS.runTickers(state, (ClientLevel)(Object)this, pos, TickSource.BLOCK_CRACKING));
        }
    }

    @WrapWithCondition(method = "addBreakingBlockEffects", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;addBreakingParticles(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private boolean polytone$skipCancelledParticles(ClientLevel level, BlockPos pos, Direction direction, BlockState state,
                                                    @Share("polytone$cancelParticles") LocalBooleanRef cancelParticles) {
        return !cancelParticles.get();
    }

    @ModifyExpressionValue(method = "addEnvironmentAttributeLayers", at = @At(value = "NEW", target = "(FFF)Lorg/joml/Vector3f;"))
    public Vector3f polytone$modifySkyLightSampler(Vector3f value) {
        Integer c = Polytone.COLORS.getSkyFlash();
        if (c != null) {
            return ARGB.vector3fFromRGB24(c);
        }
        return value;
    }
}
