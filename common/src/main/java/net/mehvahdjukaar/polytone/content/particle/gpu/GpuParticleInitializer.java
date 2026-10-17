package net.mehvahdjukaar.polytone.content.particle.gpu;

import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record GpuParticleInitializer(IBlockExp size,
                                     IBlockExp lifetime,
                                     IBlockExp red,
                                     IBlockExp green,
                                     IBlockExp blue,
                                     IBlockExp alpha,
                                     Optional<IColorGetter> colormap,
                                     IBlockExp roll,
                                     IBlockExp custom,
                                     IBlockExp dx,
                                     IBlockExp dy,
                                     IBlockExp dz) {

    public static final GpuParticleInitializer DEFAULT = new GpuParticleInitializer(
            IBlockExp.constant(0.1), IBlockExp.constant(40), IBlockExp.ONE, IBlockExp.ONE, IBlockExp.ONE, IBlockExp.ONE,
            Optional.empty(), IBlockExp.ZERO, IBlockExp.ZERO, IBlockExp.ZERO, IBlockExp.ZERO, IBlockExp.ZERO);

    public static final SchemaCodec<GpuParticleInitializer> CODEC = SchemaRecord.create(GpuParticleInitializer.class, i -> i.group(
            i.optional("size", IBlockExp.CODEC, DEFAULT.size, GpuParticleInitializer::size),
            i.optional("lifetime", IBlockExp.CODEC, DEFAULT.lifetime, GpuParticleInitializer::lifetime),
            i.optional("red", IBlockExp.CODEC, DEFAULT.red, GpuParticleInitializer::red),
            i.optional("green", IBlockExp.CODEC, DEFAULT.green, GpuParticleInitializer::green),
            i.optional("blue", IBlockExp.CODEC, DEFAULT.blue, GpuParticleInitializer::blue),
            i.optional("alpha", IBlockExp.CODEC, DEFAULT.alpha, GpuParticleInitializer::alpha),
            i.optional("colormap", SchemaCodec.wrap(Colormap.CODEC), GpuParticleInitializer::colormap),
            i.optional("roll", IBlockExp.CODEC, DEFAULT.roll, GpuParticleInitializer::roll),
            i.optional("custom", IBlockExp.CODEC, DEFAULT.custom, GpuParticleInitializer::custom),
            i.optional("dx", IBlockExp.CODEC, DEFAULT.dx, GpuParticleInitializer::dx),
            i.optional("dy", IBlockExp.CODEC, DEFAULT.dy, GpuParticleInitializer::dy),
            i.optional("dz", IBlockExp.CODEC, DEFAULT.dz, GpuParticleInitializer::dz)
    ).apply(i, GpuParticleInitializer::new));

    @Nullable
    public SpawnValues evaluate(Level level, Vec3 pos, BlockState state) {
        double life = lifetime.evaluate(level, pos, state);
        if (life <= 0) return null;
        SpawnValues v = new SpawnValues();
        v.lifetime = (float) Math.max(1, life);
        v.size = (float) size.evaluate(level, pos, state);
        v.red = (float) red.evaluate(level, pos, state);
        v.green = (float) green.evaluate(level, pos, state);
        v.blue = (float) blue.evaluate(level, pos, state);
        v.alpha = (float) alpha.evaluate(level, pos, state);
        if (colormap.isPresent()) {
            BlockPos blockPos = BlockPos.containing(pos);
            float[] tint = ColorUtils.unpack(colormap.get().sampleColorUncached(level, state, blockPos, level.getBiome(blockPos).value()));
            v.red *= tint[0];
            v.green *= tint[1];
            v.blue *= tint[2];
        }
        v.roll = (float) roll.evaluate(level, pos, state);
        v.custom = (float) custom.evaluate(level, pos, state);
        v.dx = (float) dx.evaluate(level, pos, state);
        v.dy = (float) dy.evaluate(level, pos, state);
        v.dz = (float) dz.evaluate(level, pos, state);
        return v;
    }

    //mutable so emitter overrides can be applied on top before the record is written
    public static final class SpawnValues {
        public float size, lifetime, red, green, blue, alpha, roll, custom, dx, dy, dz;

        public void override(String key, float value) {
            switch (key) {
                case "size" -> size = value;
                case "red" -> red = value;
                case "green" -> green = value;
                case "blue" -> blue = value;
                case "alpha" -> alpha = value;
                case "roll" -> roll = value;
                case "custom" -> custom = value;
                default -> {
                }
            }
        }
    }
}
