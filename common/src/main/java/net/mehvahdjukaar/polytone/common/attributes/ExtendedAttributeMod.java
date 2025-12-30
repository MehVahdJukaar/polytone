package net.mehvahdjukaar.polytone.common.attributes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.content.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.attribute.modifier.ColorModifier;
import net.minecraft.world.attribute.modifier.FloatModifier;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

public class ExtendedAttributeMod {

    public static <A, Value> Codec<A> extendValueCodec(Codec<A> argumentCodec, AttributeType<Value> type) {
        if (type == AttributeTypes.ARGB_COLOR || type == AttributeTypes.RGB_COLOR) {
            Codec<Integer> intCodec = IColorGetter.SINGLE_COLOR_OR_EXPRESSION.xmap(
                    c -> c.getColor(Blocks.AIR.defaultBlockState(),
                            Minecraft.getInstance().level,
                            ClientFrameTicker.getCameraPos(), 0),
                    IColorGetter.StaticColor::new
            );
            argumentCodec = (Codec<A>) Codec.either(argumentCodec, intCodec);
        } else if (type == AttributeTypes.FLOAT) {
            Codec<Float> flaotCodec = BlockContextExpression.CODEC
                    .xmap(e -> {
                        return (float) e.getValue(Minecraft.getInstance().level,
                                ClientFrameTicker.getCameraPos(),
                                Blocks.AIR.defaultBlockState());
                    }, ex -> BlockContextExpression.ZERO);
            argumentCodec = (Codec<A>) Codec.either(argumentCodec, flaotCodec);
        }
        return argumentCodec;
    }

    public static <V> Codec<AttributeModifier<V, ?>> extendCodec(AttributeType<V> type) {

        Codec<AttributeModifier<V, ?>> original = type.modifierCodec();
        if (type == AttributeTypes.FLOAT) {
            return Codec.withAlternative(original, (Codec<? extends AttributeModifier<V, ?>>) (Object) FLOAT_EXP_CODEC);
        }
        if (type == AttributeTypes.RGB_COLOR) {
            return Codec.withAlternative(original, (Codec<? extends AttributeModifier<V, ?>>) (Object) INT_EXP_CODEC);
        }
        if (type == AttributeTypes.ARGB_COLOR) {
            return Codec.withAlternative(original, (Codec<? extends AttributeModifier<V, ?>>) (Object) INT_EXP_CODEC);
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

    private static final BiMap<String, FloatModifier<BlockContextExpression>> FLOAT_LIBRARY = HashBiMap.create(Map.of(
            "expression", FLOAT_EXPRESSION_MOD
    ));

    public static final Codec<FloatModifier<?>> FLOAT_EXP_CODEC =
            ExtraCodecs.idResolverCodec(Codec.STRING, FLOAT_LIBRARY::get,
                    m -> FLOAT_LIBRARY.inverse().get(m));

    //TODO: same fore rgb, 1 expresion per channel or 1 global
    //TODO: add proper expression and add "THIS" parameter
    //todo: allow colromap here to blend with the values given, not just set them

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

    private static final ColorModifier<IColorGetter> INT_COLORMAP_MOD = new ColorModifier<>() {
        @Override
        public Integer apply(Integer original, IColorGetter getter) {
            return getter.getColor(Blocks.AIR.defaultBlockState(), Minecraft.getInstance().level,
                    ClientFrameTicker.getCameraPos(), 0); //TODO: give proper expression here
        }

        @Override
        public Codec<IColorGetter> argumentCodec(EnvironmentAttribute<Integer> environmentAttribute) {
            return Colormap.REFERENCE_OR_EXPRESSION;
        }

        @Override
        public LerpFunction<IColorGetter> argumentKeyframeLerp(EnvironmentAttribute<Integer> environmentAttribute) {
            return LerpFunction.ofConstant(); //expressions aren't lerpable
        }
    };


    private static final BiMap<String, ColorModifier<BlockContextExpression>> INT_LIBRARY = HashBiMap.create(Map.of(
            "expression", INT_EXPRESSION_MOD
    ));

    public static final Codec<ColorModifier<?>> INT_EXP_CODEC =
            ExtraCodecs.idResolverCodec(Codec.STRING, INT_LIBRARY::get,
                    m -> INT_LIBRARY.inverse().get(m));


}
