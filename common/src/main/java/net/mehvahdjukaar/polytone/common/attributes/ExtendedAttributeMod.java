package net.mehvahdjukaar.polytone.common.attributes;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.ICameraExp;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.AttributeTypes;

import java.util.function.Supplier;

public class ExtendedAttributeMod {

    public static <A, Value> Codec<Either<A, Supplier<A>>> addDynamicValueCodec(Codec<A> originalCodec, AttributeType<Value> type) {
        if (type == AttributeTypes.ARGB_COLOR || type == AttributeTypes.RGB_COLOR) {
            Codec<Supplier<Integer>> intCodec = Colormap.REFERENCE_OR_EXPRESSION
                    .xmap(c -> () -> c.sampleColor(
                                    Minecraft.getInstance().level,
                                    null,
                                    ClientFrameTicker.getCameraPos(), null, null),
                            supplier -> new IColorGetter.StaticColor(supplier.get()));

            return Codec.either(originalCodec, (Codec) intCodec);
        } else if (type == AttributeTypes.FLOAT || type == AttributeTypes.ANGLE_DEGREES) {
            Codec<Supplier<Float>> flaotCodec = ICameraExp.CODEC
                    .xmap(e -> () -> (float) e.evaluate(),
                            ex -> ICameraExp.ZERO);
            return Codec.either(originalCodec, (Codec) flaotCodec);
        }
        return CodecUtils.eitherLeft(originalCodec);
    }


}
