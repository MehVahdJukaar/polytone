package net.mehvahdjukaar.polytone.common.attributes;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.attributes.IExtendedEnvAttrEntry.Blend;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.attribute.modifier.ColorModifier;
import org.joml.Vector3fc;
import org.joml.Vector4fc;

import java.util.function.Supplier;

class EnvAttrEntryCodecs {

    private static final Codec<IColorGetter> EXPRESSION_COLOR = IBlockExp.CODEC.xmap(
            IColorGetter.ExpressionColor::new,
            g -> g instanceof IColorGetter.ExpressionColor(IBlockExp exp) ? exp : IBlockExp.ZERO
    );

    private static final Codec<IColorGetter> COLORMAP_OR_EXPRESSION = SchemaCodecs.withAlternative(
            SchemaCodecs.alt("reference", Polytone.COLORMAPS.byNameCodec()),
            SchemaCodecs.alt("inline", SchemaCodecs.withAlternative(
                    SchemaCodecs.alt("color", IColorGetter.SINGLE_COLOR_CODEC),
                    SchemaCodecs.alt("expression", EXPRESSION_COLOR))));

    // either just the argument (override modifier) or {modifier, argument, blend}
    static <Value, Argument> Codec<EnvironmentAttributeMap.Entry<Value, Argument>> entryCodec(
            EnvironmentAttribute<Value> attribute) {

        Codec<Argument> valueCodec = (Codec) attribute.valueCodec();
        Codec<Either<Argument, Supplier<Argument>>> shorthandCodec = valueOrDynamic(valueCodec, attribute.type(),
                colorArgOf(attribute.type()));

        //generics are just wrong here... Hoping they line up at runtime
        Codec<EnvironmentAttributeMap.Entry<Value, ?>> fullCodec = attribute.type().modifierCodec().dispatch(
                "modifier",
                EnvironmentAttributeMap.Entry::modifier,
                Util.memoize(modifier -> fullEntryCodec(attribute, modifier))
        );

        return Codec.either(shorthandCodec, (Codec<EnvironmentAttributeMap.Entry<Value, Argument>>) (Codec) fullCodec)
                .xmap(EnvAttrEntryCodecs::fromShorthandOrFull, EnvAttrEntryCodecs::toShorthandOrFull);
    }

    private static <Value, Argument> MapCodec<EnvironmentAttributeMap.Entry<Value, Argument>> fullEntryCodec(
            EnvironmentAttribute<Value> attribute, AttributeModifier<Value, Argument> modifier) {

        Codec<Either<Argument, Supplier<Argument>>> argumentCodec =
                valueOrDynamic(modifier.argumentCodec(attribute), attribute.type(), colorArgOf(attribute.type(), modifier));

        return RecordCodecBuilder.mapCodec(i -> i.group(
                argumentCodec.fieldOf("argument").forGetter(EnvAttrEntryCodecs::argumentOrSupplier),
                Codec.BOOL.optionalFieldOf("biome_blend", true).forGetter(e -> IExtendedEnvAttrEntry.of(e).polytone$getBlend().biome()),
                Codec.BOOL.optionalFieldOf("time_blend", true).forGetter(e -> IExtendedEnvAttrEntry.of(e).polytone$getBlend().time())
        ).apply(i, (argument, biomeBlend, timeBlend) -> createEntry(argument, modifier, new Blend(biomeBlend, timeBlend))));
    }

    private static <Value, Argument> EnvironmentAttributeMap.Entry<Value, Argument> fromShorthandOrFull(
            Either<Either<Argument, Supplier<Argument>>, EnvironmentAttributeMap.Entry<Value, Argument>> shorthandOrFull) {
        return shorthandOrFull.map(
                argument -> createEntry(argument, (AttributeModifier<Value, Argument>) AttributeModifier.override(), Blend.DEFAULT),
                entry -> entry
        );
    }

    private static <Value, Argument> Either<Either<Argument, Supplier<Argument>>, EnvironmentAttributeMap.Entry<Value, Argument>> toShorthandOrFull(
            EnvironmentAttributeMap.Entry<Value, Argument> entry) {
        //an entry that opted out of blending has to keep the object form, that's where the flag lives
        boolean fitsShorthand = entry.modifier() == AttributeModifier.override()
                && IExtendedEnvAttrEntry.of(entry).polytone$getBlend().equals(Blend.DEFAULT);
        if (fitsShorthand) return Either.left(argumentOrSupplier(entry));
        return Either.right(entry);
    }

    private static <Argument> Either<Argument, Supplier<Argument>> argumentOrSupplier(
            EnvironmentAttributeMap.Entry<?, Argument> entry) {
        Supplier<Argument> supplier = IExtendedEnvAttrEntry.of(entry).polytone$getArgumentSupplier();
        if (supplier != null) return Either.right(supplier);
        return Either.left(entry.argument());
    }

    private static <Value, Argument> EnvironmentAttributeMap.Entry<Value, Argument> createEntry(
            Either<Argument, Supplier<Argument>> argumentOrSupplier, AttributeModifier<Value, Argument> modifier,
            Blend blend) {
        return argumentOrSupplier.map(
                argument -> new EnvironmentAttributeMap.Entry<>(argument, modifier),
                supplier -> IExtendedEnvAttrEntry.createDynamic(supplier, modifier, blend)
        );
    }

    // Colors are vectors: Vector3fc for rgb, Vector4fc for argb
    private enum ColorArg {NONE, RGB, ARGB}

    private static ColorArg colorArgOf(AttributeType<?> type) {
        if (type == AttributeTypes.RGB_COLOR) return ColorArg.RGB;
        if (type == AttributeTypes.ARGB_COLOR) return ColorArg.ARGB;
        return ColorArg.NONE;
    }

    private static ColorArg colorArgOf(AttributeType<?> type, AttributeModifier<?, ?> modifier) {
        ColorArg valueArg = colorArgOf(type);
        if (valueArg == ColorArg.NONE || modifier == AttributeModifier.override()) return valueArg;
        if (modifier instanceof ColorModifier.ArgbModifier<?>) return ColorArg.ARGB;
        if (modifier instanceof ColorModifier.RgbModifier<?>) return ColorArg.RGB;
        return ColorArg.NONE;
    }

    private static Object colorToArgument(int color, ColorArg colorArg) {
        return colorArg == ColorArg.ARGB ? ARGB.vector4fFromARGB32(color) : ARGB.vector3fFromRGB24(color);
    }

    private static int argumentToColor(Object argument) {
        if (argument instanceof Vector4fc v) return ARGB.colorFromVector4f(v);
        if (argument instanceof Vector3fc v) return ARGB.colorFromVector3f(v);
        return 0;
    }

    // Allows a Colormap or an Expression to be used wherever a color or a float attribute value is expected
    private static <A, Value> Codec<Either<A, Supplier<A>>> valueOrDynamic(Codec<A> valueCodec, AttributeType<Value> type,
                                                                         ColorArg colorArg) {
        if (colorArg != ColorArg.NONE) {
            Codec<Supplier<Object>> colormapCodec = COLORMAP_OR_EXPRESSION.xmap(
                    colormap -> () -> colorToArgument(DynamicAttributeContext.sampleColor(colormap), colorArg),
                    supplier -> new IColorGetter.StaticColor(argumentToColor(supplier.get())));
            return Codec.either(valueCodec, (Codec) colormapCodec);
        }
        if (type == AttributeTypes.FLOAT || type == AttributeTypes.ANGLE_DEGREES) {
            Codec<Supplier<Float>> expressionCodec = IBlockExp.CODEC_LEGACY.xmap(
                    exp -> () -> DynamicAttributeContext.evaluate(exp),
                    supplier -> IBlockExp.ZERO);
            return Codec.either(valueCodec, (Codec) expressionCodec);
        }
        return SchemaCodecs.eitherLeft(valueCodec);
    }
}
