package net.mehvahdjukaar.polytone.mixins.compat;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.shaders.light.LightShaderPatcher;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(ChunkMeshBufferBuilder.class)
public abstract class SodiumMeshBufferBuilderMixin {

    @Unique
    private boolean polytone$writesFaceBits;

    @Inject(method = "<init>", remap = false, at = @At("TAIL"))
    private void polytone$checkFaceBits(ChunkVertexType vertexType, int initialCapacity, CallbackInfo ci) {
        polytone$writesFaceBits = Polytone.CONFIGS.coloredLightsBackend.get().tintsVanillaBlockLight()
                && !CompatHandler.irisShaderPackActive();
    }

    @ModifyArg(require = 2, method = {"push([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)V",
            "writeExternal"}, remap = false, index = 1,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder;write(JI[Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)J"))
    private int polytone$hackilyAddDirectionBits(int materialBits, @Local(argsOnly = true) ChunkVertexEncoder.Vertex[] quad) {
        if (!polytone$writesFaceBits) return materialBits;
        Direction face = polytone$faceOf(quad);
        if (face == null) return materialBits;
        //sorry if somebody else was using these
        return materialBits | (face.ordinal() + 1) << LightShaderPatcher.SODIUM_MATERIAL_DIRECTION_BITS_SHIFT;
    }

    @Unique
    private static Direction polytone$faceOf(ChunkVertexEncoder.Vertex[] quad) {
        //cross of the two diagonals
        float ax = quad[2].x - quad[0].x, ay = quad[2].y - quad[0].y, az = quad[2].z - quad[0].z;
        float bx = quad[3].x - quad[1].x, by = quad[3].y - quad[1].y, bz = quad[3].z - quad[1].z;
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        if (nx * nx + ny * ny + nz * nz < 1.0e-8f) return null;
        return Direction.getNearest(nx, ny, nz);
    }
}
