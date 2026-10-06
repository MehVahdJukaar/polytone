package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererLightMixin<T extends Entity> {

    @Shadow
    protected abstract int getBlockLightLevel(T entity, BlockPos pos);

    @Shadow
    protected abstract int getSkyLightLevel(T entity, BlockPos pos);

    //attempt to improve vanilla look
    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    private void polytone$blendLightAroundProbe(T entity, float partialTicks, CallbackInfoReturnable<Integer> cir) {
        boolean isSmooth = switch (Polytone.CONFIGS.smoothEntityLighting.get()) {
            case ALL -> true;
            case LOCAL_PLAYER -> entity == Minecraft.getInstance().player;
            case OFF -> false;
        };
        if (!isSmooth) return;
        Vec3 probe = entity.getLightProbePosition(partialTicks);
        double px = probe.x - 0.5;
        double py = probe.y - 0.5;
        double pz = probe.z - 0.5;
        int x0 = Mth.floor(px);
        int y0 = Mth.floor(py);
        int z0 = Mth.floor(pz);
        float fx = (float) (px - x0);
        float fy = (float) (py - y0);
        float fz = (float) (pz - z0);

        Level level = entity.level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        float block = 0;
        float sky = 0;
        float totalWeight = 0;
        for (int dx = 0; dx <= 1; dx++) {
            float wx = dx == 1 ? fx : 1 - fx;
            for (int dy = 0; dy <= 1; dy++) {
                float wy = dy == 1 ? fy : 1 - fy;
                for (int dz = 0; dz <= 1; dz++) {
                    float wz = dz == 1 ? fz : 1 - fz;
                    pos.set(x0 + dx, y0 + dy, z0 + dz);
                    //solid blocks have 0 light inside, would darken anything next to a wall
                    if (level.getBlockState(pos).isSolidRender(level, pos)) continue;
                    float w = wx * wy * wz;
                    block += w * getBlockLightLevel(entity, pos);
                    sky += w * getSkyLightLevel(entity, pos);
                    totalWeight += w;
                }
            }
        }
        if (totalWeight < 0.001f) return;
        int blockCoord = Math.round(block / totalWeight * 16);
        int skyCoord = Math.round(sky / totalWeight * 16);
        cir.setReturnValue(blockCoord | skyCoord << 16);
    }
}
