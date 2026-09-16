package net.mehvahdjukaar.polytone.common.attributes;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.modifier.AttributeModifier;

import java.util.function.Supplier;

class EnvAttrEntryCodecs {

    // either just the argument (override modifier) or {modifier, argument, blend}
    static <Value, Argument> Codec<EnvironmentAttributeMap.Entry<Value, Argument>> entryCodec(
            EnvironmentAttribute<Value> attribute) {

        Codec<Argument> valueCodec = (Codec) attribute.valueCodec();
        Codec<Either<Argument, Supplier<Argument>>> shorthandCodec = valueOrDynamic(valueCodec, attribute.type());

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
                valueOrDynamic(modifier.argumentCodec(attribute), attribute.type());

        return RecordCodecBuilder.mapCodec(i -> i.group(
                argumentCodec.fieldOf("argument").forGetter(EnvAttrEntryCodecs::argumentOrSupplier),
                Codec.BOOL.optionalFieldOf("blend", true).forGetter(e -> IExtendedEnvAttrEntry.of(e).polytone$shouldBlend())
        ).apply(i, (argument, blend) -> createEntry(argument, modifier, blend)));
    }

    private static <Value, Argument> EnvironmentAttributeMap.Entry<Value, Argument> fromShorthandOrFull(
            Either<Either<Argument, Supplier<Argument>>, EnvironmentAttributeMap.Entry<Value, Argument>> shorthandOrFull) {
        return shorthandOrFull.map(
                argument -> createEntry(argument, (AttributeModifier<Value, Argument>) AttributeModifier.override(), true),
                entry -> entry
        );
    }

    private static <Value, Argument> Either<Either<Argument, Supplier<Argument>>, EnvironmentAttributeMap.Entry<Value, Argument>> toShorthandOrFull(
            EnvironmentAttributeMap.Entry<Value, Argument> entry) {
        //an entry that opted out of blending has to keep the object form, that's where the flag lives
        boolean fitsShorthand = entry.modifier() == AttributeModifier.override()
                && IExtendedEnvAttrEntry.of(entry).polytone$shouldBlend();
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
            boolean blend) {
        return argumentOrSupplier.map(
                argument -> new EnvironmentAttributeMap.Entry<>(argument, modifier),
                supplier -> IExtendedEnvAttrEntry.createDynamic(supplier, modifier, blend)
        );
    }

    // Allows a Colormap or an Expression to be used wherever a color or a float attribute value is expected
    private static <A, Value> Codec<Either<A, Supplier<A>>> valueOrDynamic(Codec<A> valueCodec, AttributeType<Value> type) {
        if (type == AttributeTypes.ARGB_COLOR || type == AttributeTypes.RGB_COLOR) {
            Codec<Supplier<Integer>> colormapCodec = Colormap.REFERENCE_OR_EXPRESSION.xmap(
                    colormap -> () -> DynamicAttributeContext.sampleColor(colormap),
                    supplier -> new IColorGetter.StaticColor(supplier.get()));
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
