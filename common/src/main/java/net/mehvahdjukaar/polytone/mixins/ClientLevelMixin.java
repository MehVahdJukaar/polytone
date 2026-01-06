package net.mehvahdjukaar.polytone.mixins;

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
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level {

    protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }

    @WrapOperation(method = "doAnimateTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    public void polytone$extraParticles(Block instance, BlockState state, Level level, BlockPos pos, RandomSource random,
                                        Operation<Void> original) {
        boolean cancels = Polytone.BLOCK_MODIFIERS.runTickers(state, level, pos, TickSource.ANIMATE_TICK);
        if (!cancels) {
            original.call(instance, state, level, pos, random);
        }
    }

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    public void polytone$addExtraDestroyParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!state.isAir()) {
            //TODO: add more tick sources
            boolean cancels = Polytone.BLOCK_MODIFIERS.runTickers(state, this, pos, TickSource.BLOCK_BROKEN);
            if (cancels) {
                ci.cancel();
            }
        }
    }


    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    public void polytone$addExtraBreakingParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        BlockState state = this.getBlockState(pos);
        if (!state.isAir()) {
            boolean cancels = Polytone.BLOCK_MODIFIERS.runTickers(state, this, pos, TickSource.BLOCK_BROKEN);
            if (cancels) {
                ci.cancel();
            }
        }
    }
    @ModifyExpressionValue(method = "addEnvironmentAttributeLayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;color(III)I"))
    public int polytone$modifySkyLightSampler(int value) {
        Integer c = Polytone.COLORS.getSkyFlash();
        if (c != null) {
            return c;
        }
        return value;
    }
}
