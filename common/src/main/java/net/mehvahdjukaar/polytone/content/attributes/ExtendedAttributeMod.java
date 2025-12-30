package net.mehvahdjukaar.polytone.content.attributes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.content.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.attribute.modifier.ColorModifier;
import net.minecraft.world.attribute.modifier.FloatModifier;
import net.minecraft.world.level.block.Blocks;

public class ExtendedAttributeMod {

    public static <V> Codec<AttributeModifier<V, ?>> extendCodec(AttributeType<V> type) {

        Codec<AttributeModifier<V, ?>> original = type.modifierCodec();
        if (type == AttributeTypes.FLOAT) {
            return Codec.withAlternative(original, (Codec<? extends AttributeModifier<V, ?>>)(Object) FLOAT_EXP_CODEC);
        }
        if (type == AttributeTypes.RGB_COLOR) {
            return Codec.withAlternative(original, (Codec<? extends AttributeModifier<V, ?>>)(Object) INT_EXP_CODEC);
        }
        if (type == AttributeTypes.ARGB_COLOR) {
            return Codec.withAlternative(original, (Codec<? extends AttributeModifier<V, ?>>)(Object) INT_EXP_CODEC);
        }
        return original;
    }


    private static final FloatModifier<BlockContextExpression> FLOAT_EXPRESSION_MOD = new FloatModifier<>() {
        @Override
        public Float apply(Float object, BlockContextExpression exp) {
            return (float) exp.getValue(Minecraft.getInstance().level, ClientFrameTicker.getCameraPos(),
                    Blocks.AIR.defaultBlockState()); //TODO: give proper expression here
        }

        @Override
        public Codec<BlockContextExpression> argumentCodec(EnvironmentAttribute<Float> environmentAttribute) {
            return BlockContextExpression.CODEC;
        }

        @Override
        public LerpFunction<BlockContextExpression> argumentKeyframeLerp(EnvironmentAttribute<Float> environmentAttribute) {
            return LerpFunction.ofConstant(); //expressions aren't lerpable
        }
    };

    public static final Codec<AttributeModifier<?, ?>> FLOAT_EXP_CODEC = Codec.STRING.flatXmap(s -> {
        if (s.equals("expression")) {
            return DataResult.success(FLOAT_EXPRESSION_MOD);
        } else return DataResult.error(() -> "Unknown key" + s);
    }, mod -> DataResult.success("expression"));

    //TODO: same fore rgb, 1 expresion per channel or 1 global
    //TODO: add proper expression and add "THIS" parameter

    private static final ColorModifier<BlockContextExpression> INT_EXPRESSION_MOD = new ColorModifier<>() {
        @Override
        public Integer apply(Integer original, BlockContextExpression exp) {
            return (int) exp.getValue(Minecraft.getInstance().level, ClientFrameTicker.getCameraPos(),
                    Blocks.AIR.defaultBlockState()); //TODO: give proper expression here
        }

        @Override
        public Codec<BlockContextExpression> argumentCodec(EnvironmentAttribute<Integer> environmentAttribute) {
            return BlockContextExpression.CODEC;
        }

        @Override
        public LerpFunction<BlockContextExpression> argumentKeyframeLerp(EnvironmentAttribute<Integer> environmentAttribute) {
            return LerpFunction.ofConstant(); //expressions aren't lerpable
        }
    };


    public static final Codec<ColorModifier<BlockContextExpression>> INT_EXP_CODEC = Codec.STRING.flatXmap(s -> {
        if (s.equals("expression")) {
            return DataResult.success(INT_EXPRESSION_MOD);
        } else return DataResult.error(() -> "Unknown key" + s);
    }, mod -> DataResult.success("expression"));
}
