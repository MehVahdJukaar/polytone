package net.mehvahdjukaar.polytone.content.particle.gpu;

import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public record GpuParticleInitializer(IBlockExp size,
                                     IBlockExp lifetime,
                                     IBlockExp red,
                                     IBlockExp green,
                                     IBlockExp blue,
                                     IBlockExp alpha,
                                     Optional<IColorGetter> colormap,
                                     IBlockExp roll,
                                     IBlockExp custom) {

    public static final GpuParticleInitializer DEFAULT = new GpuParticleInitializer(
            IBlockExp.constant(0.1), IBlockExp.constant(40), IBlockExp.ONE, IBlockExp.ONE, IBlockExp.ONE, IBlockExp.ONE,
            Optional.empty(), IBlockExp.ZERO, IBlockExp.ZERO);

    public static final SchemaCodec<GpuParticleInitializer> CODEC = SchemaRecord.create(GpuParticleInitializer.class, i -> i.group(
            i.optional("size", IBlockExp.MVEL_CODEC, DEFAULT.size, GpuParticleInitializer::size),
            i.optional("lifetime", IBlockExp.MVEL_CODEC, DEFAULT.lifetime, GpuParticleInitializer::lifetime),
            i.optional("red", IBlockExp.MVEL_CODEC, DEFAULT.red, GpuParticleInitializer::red),
            i.optional("green", IBlockExp.MVEL_CODEC, DEFAULT.green, GpuParticleInitializer::green),
            i.optional("blue", IBlockExp.MVEL_CODEC, DEFAULT.blue, GpuParticleInitializer::blue),
            i.optional("alpha", IBlockExp.MVEL_CODEC, DEFAULT.alpha, GpuParticleInitializer::alpha),
            i.optional("colormap", Colormap.CODEC, GpuParticleInitializer::colormap),
            i.optional("roll", IBlockExp.MVEL_CODEC, DEFAULT.roll, GpuParticleInitializer::roll),
            i.optional("custom", IBlockExp.MVEL_CODEC, DEFAULT.custom, GpuParticleInitializer::custom)
    ).apply(i, GpuParticleInitializer::new));

    public SpawnValues evaluate(ClientLevel level, Vec3 pos, BlockState state) {
        SpawnValues v = new SpawnValues();
        v.size = (float) size.evaluate(level, pos, state);
        v.lifetime = (float) Math.max(1, lifetime.evaluate(level, pos, state));
        v.red = (float) red.evaluate(level, pos, state);
        v.green = (float) green.evaluate(level, pos, state);
        v.blue = (float) blue.evaluate(level, pos, state);
        v.alpha = (float) alpha.evaluate(level, pos, state);
        if (colormap.isPresent()) {
            float[] tint = ColorUtils.unpack(colormap.get().colorInWorld(state, level, BlockPos.containing(pos)));
            v.red *= tint[0];
            v.green *= tint[1];
            v.blue *= tint[2];
        }
        v.roll = (float) roll.evaluate(level, pos, state);
        v.custom = (float) custom.evaluate(level, pos, state);
        return v;
    }

    // mutable so emitter overrides can be applied on top before the record is written
    public static final class SpawnValues {
        public float size, lifetime, red, green, blue, alpha, roll, custom;

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
