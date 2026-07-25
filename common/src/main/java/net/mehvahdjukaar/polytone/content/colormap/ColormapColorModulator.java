package net.mehvahdjukaar.polytone.content.colormap;

import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IColormapModExp;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ColormapColorModulator {

    public static SchemaCodec<ColormapColorModulator> CODEC = SchemaRecord.create(ColormapColorModulator.class, i ->
            i.group(
                    i.optional("red", IColormapModExp.CODEC, c -> c.red),
                    i.optional("green", IColormapModExp.CODEC, c -> c.green),
                    i.optional("blue", IColormapModExp.CODEC, c -> c.blue)
            ).apply(i, ColormapColorModulator::new));

    private final Optional<IColormapModExp> red;
    private final Optional<IColormapModExp> green;
    private final Optional<IColormapModExp> blue;

    protected ColormapColorModulator(Optional<IColormapModExp> red,
                                     Optional<IColormapModExp> green,
                                     Optional<IColormapModExp> blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public ColormapColorModulator createConcurrent() {
        return new ColormapColorModulator(
                red.map(IColormapModExp::createConcurrent),
                green.map(IColormapModExp::createConcurrent),
                blue.map(IColormapModExp::createConcurrent)
        );
    }

    public int evaluate(int original, @Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos, @Nullable Biome biome, @Nullable BiomeIdMapper mapper, @Nullable ItemStack stack) {
        float[] values = ColorUtils.unpack(original);
        float red = values[0];
        float green = values[1];
        float blue = values[2];

        float newRed = this.red.map(exp -> exp.evaluate(red, green, blue, level, state, pos, biome, mapper, stack)).orElse(red);
        float newGreen = this.green.map(exp -> exp.evaluate(red, green, blue, level, state, pos, biome, mapper, stack)).orElse(green);
        float newBlue = this.blue.map(exp -> exp.evaluate(red, green, blue, level, state, pos, biome, mapper, stack)).orElse(blue);
        return ColorUtils.pack(newRed, newGreen, newBlue);
    }

}